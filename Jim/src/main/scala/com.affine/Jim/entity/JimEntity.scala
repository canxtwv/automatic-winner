package com.affine.Jim.entity

import java.time.LocalDateTime
import java.util.UUID
import com.affine.Jim.data._
import scala.concurrent.Future

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

object JimEntity {

  sealed trait Command
  // command
  final case class EntityCreateJim(id: UUID, create: CreateJim)(val replyTo: ActorRef[JimCreated]) extends Command
  // query
  final case class EntityGetJim(get: GetJim)(val replyTo: ActorRef[Jim]) extends Command
  final case class EntityGetState(id: UUID)(val replyTo: ActorRef[JimState]) extends Command

  val entityTypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("JimEntity")

  def behavior(entityId: String): Behavior[Command] =
    EventSourcedBehavior[Command, JimEvent, JimState](
    persistenceId = PersistenceId(entityId),
    emptyState =  JimState(None),
    commandHandler,
    eventHandler)

  private val commandHandler: (JimState, Command) => Effect[JimEvent, JimState] = { (state, command) =>
    command match {
      case x: EntityCreateJim =>
        val id = x.id
        val entity = Jim(id, x.create.data)
        val created = JimCreated(entity)
        Effect.persist(created).thenRun(_ => x.replyTo.tell(created))

      case x: EntityGetJim =>
        state.entity.map(x.replyTo.tell)
        Effect.none

      case x: EntityGetState =>
        x.replyTo.tell(state)
        Effect.none

      case _ => Effect.unhandled
    }
  }

  private val eventHandler: (JimState, JimEvent) => JimState = { (state, event) =>
    state match {
      case state: JimState =>
        event match {
        case JimCreated(module) =>
          JimState(Some(module))
        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
      }
      case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
    }
  }

}


class JimEntityDatabase(system: ActorSystem[_])(implicit val ex: ExecutionContext)
  extends JimDatabase with CQRSDatabase[JimState]{
  import akka.util.Timeout
  import scala.concurrent.duration._
  private implicit val askTimeout: Timeout = Timeout(5.seconds)

  val TypeKey = JimEntity.entityTypeKey
  val sharding =  ClusterSharding(system)
  val psEntities: ActorRef[ShardingEnvelope[JimEntity.Command]] =
    sharding.init(Entity(typeKey = TypeKey,
      createBehavior = ctx => JimEntity.behavior(ctx.entityId))
      .withSettings(ClusterShardingSettings(system)))

  def entity(id: String) =
    sharding.entityRefFor(JimEntity.entityTypeKey, id)

  override def createJim(x: CreateJim): Future[JimCreated] = {
    val id = UUID.randomUUID()
    entity(id.toString) ? JimEntity.EntityCreateJim(id, x)
  }

  override def getJim(x: GetJim): Future[Jim] =
    entity(x.id.toString) ? JimEntity.EntityGetJim(x)

  override def getState(id: String): Future[JimState] =
    entity(id) ? JimEntity.EntityGetState(UUID.fromString(id))
}
