package com.affine.Zare.entity

import java.time.LocalDateTime
import java.util.UUID
import com.affine.Zare.data._
import scala.concurrent.Future

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

object ZareEntity {

  sealed trait Command
  // command
  final case class EntityCreateZare(id: UUID, create: CreateZare)(val replyTo: ActorRef[ZareCreated]) extends Command
  // query
  final case class EntityGetZare(get: GetZare)(val replyTo: ActorRef[Zare]) extends Command
  final case class EntityGetState(id: UUID)(val replyTo: ActorRef[ZareState]) extends Command

  val entityTypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("ZareEntity")

  def behavior(entityId: String): Behavior[Command] =
    EventSourcedBehavior[Command, ZareEvent, ZareState](
    persistenceId = PersistenceId(entityId),
    emptyState =  ZareState(None),
    commandHandler,
    eventHandler)

  private val commandHandler: (ZareState, Command) => Effect[ZareEvent, ZareState] = { (state, command) =>
    command match {
      case x: EntityCreateZare =>
        val id = x.id
        val entity = Zare(id, x.create.data)
        val created = ZareCreated(entity)
        Effect.persist(created).thenRun(_ => x.replyTo.tell(created))

      case x: EntityGetZare =>
        state.entity.map(x.replyTo.tell)
        Effect.none

      case x: EntityGetState =>
        x.replyTo.tell(state)
        Effect.none

      case _ => Effect.unhandled
    }
  }

  private val eventHandler: (ZareState, ZareEvent) => ZareState = { (state, event) =>
    state match {
      case state: ZareState =>
        event match {
        case ZareCreated(module) =>
          ZareState(Some(module))
        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
      }
      case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
    }
  }

}


class ZareEntityDatabase(system: ActorSystem[_])(implicit val ex: ExecutionContext)
  extends ZareDatabase with CQRSDatabase[ZareState]{
  import akka.util.Timeout
  import scala.concurrent.duration._
  private implicit val askTimeout: Timeout = Timeout(5.seconds)

  val TypeKey = ZareEntity.entityTypeKey
  val sharding =  ClusterSharding(system)
  val psEntities: ActorRef[ShardingEnvelope[ZareEntity.Command]] =
    sharding.init(Entity(typeKey = TypeKey,
      createBehavior = ctx => ZareEntity.behavior(ctx.entityId))
      .withSettings(ClusterShardingSettings(system)))

  def entity(id: String) =
    sharding.entityRefFor(ZareEntity.entityTypeKey, id)

  override def createZare(x: CreateZare): Future[ZareCreated] = {
    val id = UUID.randomUUID()
    entity(id.toString) ? ZareEntity.EntityCreateZare(id, x)
  }

  override def getZare(x: GetZare): Future[Zare] =
    entity(x.id.toString) ? ZareEntity.EntityGetZare(x)

  override def getState(id: String): Future[ZareState] =
    entity(id) ? ZareEntity.EntityGetState(UUID.fromString(id))
}
