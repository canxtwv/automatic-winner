package com.affine.Tea.entity

import java.time.LocalDateTime
import java.util.UUID
import com.affine.Tea.data._
import scala.concurrent.Future

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

object TeaEntity {

  sealed trait Command
  // command
  final case class EntityCreateTea(id: UUID, create: CreateTea)(val replyTo: ActorRef[TeaCreated]) extends Command
  // query
  final case class EntityGetTea(get: GetTea)(val replyTo: ActorRef[Tea]) extends Command
  final case class EntityGetState(id: UUID)(val replyTo: ActorRef[TeaState]) extends Command

  val entityTypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("TeaEntity")

  def behavior(entityId: String): Behavior[Command] =
    EventSourcedBehavior[Command, TeaEvent, TeaState](
    persistenceId = PersistenceId(entityId),
    emptyState =  TeaState(None),
    commandHandler,
    eventHandler)

  private val commandHandler: (TeaState, Command) => Effect[TeaEvent, TeaState] = { (state, command) =>
    command match {
      case x: EntityCreateTea =>
        val id = x.id
        val entity = Tea(id, x.create.data)
        val created = TeaCreated(entity)
        Effect.persist(created).thenRun(_ => x.replyTo.tell(created))

      case x: EntityGetTea =>
        state.entity.map(x.replyTo.tell)
        Effect.none

      case x: EntityGetState =>
        x.replyTo.tell(state)
        Effect.none

      case _ => Effect.unhandled
    }
  }

  private val eventHandler: (TeaState, TeaEvent) => TeaState = { (state, event) =>
    state match {
      case state: TeaState =>
        event match {
        case TeaCreated(module) =>
          TeaState(Some(module))
        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
      }
      case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
    }
  }

}


class TeaEntityDatabase(system: ActorSystem[_])(implicit val ex: ExecutionContext)
  extends TeaDatabase with CQRSDatabase[TeaState]{
  import akka.util.Timeout
  import scala.concurrent.duration._
  private implicit val askTimeout: Timeout = Timeout(5.seconds)

  val TypeKey = TeaEntity.entityTypeKey
  val sharding =  ClusterSharding(system)
  val psEntities: ActorRef[ShardingEnvelope[TeaEntity.Command]] =
    sharding.init(Entity(typeKey = TypeKey,
      createBehavior = ctx => TeaEntity.behavior(ctx.entityId))
      .withSettings(ClusterShardingSettings(system)))

  def entity(id: String) =
    sharding.entityRefFor(TeaEntity.entityTypeKey, id)

  override def createTea(x: CreateTea): Future[TeaCreated] = {
    val id = UUID.randomUUID()
    entity(id.toString) ? TeaEntity.EntityCreateTea(id, x)
  }

  override def getTea(x: GetTea): Future[Tea] =
    entity(x.id.toString) ? TeaEntity.EntityGetTea(x)

  override def getState(id: String): Future[TeaState] =
    entity(id) ? TeaEntity.EntityGetState(UUID.fromString(id))
}
