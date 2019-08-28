package com.affine.Demo.entity

import java.time.LocalDateTime
import java.util.UUID
import com.affine.Demo.data._
import scala.concurrent.Future

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

object DemoEntity {

  sealed trait Command
  // command
  final case class EntityCreateDemo(id: UUID, create: CreateDemo)(val replyTo: ActorRef[DemoCreated]) extends Command
  // query
  final case class EntityGetDemo(get: GetDemo)(val replyTo: ActorRef[Demo]) extends Command
  final case class EntityGetState(id: UUID)(val replyTo: ActorRef[DemoState]) extends Command

  val entityTypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("DemoEntity")

  def behavior(entityId: String): Behavior[Command] =
    EventSourcedBehavior[Command, DemoEvent, DemoState](
    persistenceId = PersistenceId(entityId),
    emptyState =  DemoState(None),
    commandHandler,
    eventHandler)

  private val commandHandler: (DemoState, Command) => Effect[DemoEvent, DemoState] = { (state, command) =>
    command match {
      case x: EntityCreateDemo =>
        val id = x.id
        val entity = Demo(id, x.create.data)
        val created = DemoCreated(entity)
        Effect.persist(created).thenRun(_ => x.replyTo.tell(created))

      case x: EntityGetDemo =>
        state.entity.map(x.replyTo.tell)
        Effect.none

      case x: EntityGetState =>
        x.replyTo.tell(state)
        Effect.none

      case _ => Effect.unhandled
    }
  }

  private val eventHandler: (DemoState, DemoEvent) => DemoState = { (state, event) =>
    state match {
      case state: DemoState =>
        event match {
        case DemoCreated(module) =>
          DemoState(Some(module))
        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
      }
      case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
    }
  }

}


class DemoEntityDatabase(system: ActorSystem[_])(implicit val ex: ExecutionContext)
  extends DemoDatabase with CQRSDatabase[DemoState]{
  import akka.util.Timeout
  import scala.concurrent.duration._
  private implicit val askTimeout: Timeout = Timeout(5.seconds)

  val TypeKey = DemoEntity.entityTypeKey
  val sharding =  ClusterSharding(system)
  val psEntities: ActorRef[ShardingEnvelope[DemoEntity.Command]] =
    sharding.init(Entity(typeKey = TypeKey,
      createBehavior = ctx => DemoEntity.behavior(ctx.entityId))
      .withSettings(ClusterShardingSettings(system)))

  def entity(id: String) =
    sharding.entityRefFor(DemoEntity.entityTypeKey, id)

  override def createDemo(x: CreateDemo): Future[DemoCreated] = {
    val id = UUID.randomUUID()
    entity(id.toString) ? DemoEntity.EntityCreateDemo(id, x)
  }

  override def getDemo(x: GetDemo): Future[Demo] =
    entity(x.id.toString) ? DemoEntity.EntityGetDemo(x)

  override def getState(id: String): Future[DemoState] =
    entity(id) ? DemoEntity.EntityGetState(UUID.fromString(id))
}
