package com.affine.Zip.entity

import java.time.LocalDateTime
import java.util.UUID
import com.affine.Zip.data._
import scala.concurrent.Future

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

object ZipEntity {

  sealed trait Command
  // command
  final case class EntityCreateZip(id: UUID, create: CreateZip)(val replyTo: ActorRef[ZipCreated]) extends Command
  // query
  final case class EntityGetZip(get: GetZip)(val replyTo: ActorRef[Zip]) extends Command
  final case class EntityGetState(id: UUID)(val replyTo: ActorRef[ZipState]) extends Command

  val entityTypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("ZipEntity")

  def behavior(entityId: String): Behavior[Command] =
    EventSourcedBehavior[Command, ZipEvent, ZipState](
    persistenceId = PersistenceId(entityId),
    emptyState =  ZipState(None),
    commandHandler,
    eventHandler)

  private val commandHandler: (ZipState, Command) => Effect[ZipEvent, ZipState] = { (state, command) =>
    command match {
      case x: EntityCreateZip =>
        val id = x.id
        val entity = Zip(id, x.create.data)
        val created = ZipCreated(entity)
        Effect.persist(created).thenRun(_ => x.replyTo.tell(created))

      case x: EntityGetZip =>
        state.entity.map(x.replyTo.tell)
        Effect.none

      case x: EntityGetState =>
        x.replyTo.tell(state)
        Effect.none

      case _ => Effect.unhandled
    }
  }

  private val eventHandler: (ZipState, ZipEvent) => ZipState = { (state, event) =>
    state match {
      case state: ZipState =>
        event match {
        case ZipCreated(module) =>
          ZipState(Some(module))
        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
      }
      case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
    }
  }

}


class ZipEntityDatabase(system: ActorSystem[_])(implicit val ex: ExecutionContext)
  extends ZipDatabase with CQRSDatabase[ZipState]{
  import akka.util.Timeout
  import scala.concurrent.duration._
  private implicit val askTimeout: Timeout = Timeout(5.seconds)

  val TypeKey = ZipEntity.entityTypeKey
  val sharding =  ClusterSharding(system)
  val psEntities: ActorRef[ShardingEnvelope[ZipEntity.Command]] =
    sharding.init(Entity(typeKey = TypeKey,
      createBehavior = ctx => ZipEntity.behavior(ctx.entityId))
      .withSettings(ClusterShardingSettings(system)))

  def entity(id: String) =
    sharding.entityRefFor(ZipEntity.entityTypeKey, id)

  override def createZip(x: CreateZip): Future[ZipCreated] = {
    val id = UUID.randomUUID()
    entity(id.toString) ? ZipEntity.EntityCreateZip(id, x)
  }

  override def getZip(x: GetZip): Future[Zip] =
    entity(x.id.toString) ? ZipEntity.EntityGetZip(x)

  override def getState(id: String): Future[ZipState] =
    entity(id) ? ZipEntity.EntityGetState(UUID.fromString(id))
}
