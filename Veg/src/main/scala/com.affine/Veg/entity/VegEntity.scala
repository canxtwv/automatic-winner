package com.affine.Veg.entity

import java.time.LocalDateTime
import java.util.UUID
import com.affine.Veg.data._
import scala.concurrent.Future

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import io.surfkit.typebus.bus.Publisher
import io.surfkit.typebus.entity.EntityDb

object VegEntity {

  sealed trait Command
  // command
  final case class EntityCreateVeg(id: UUID, create: CreateVeg)(val replyTo: ActorRef[VegCreated]) extends Command
  final case class EntityModifyState(state: VegState)(val replyTo: ActorRef[VegCompensatingActionPerformed]) extends Command
  // query
  final case class EntityGetVeg(get: GetVeg)(val replyTo: ActorRef[Veg]) extends Command
  final case class EntityGetState(id: UUID)(val replyTo: ActorRef[VegState]) extends Command

  val entityTypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("VegEntity")

  def behavior(entityId: String): Behavior[Command] =
    EventSourcedBehavior[Command, VegEvent, VegState](
    persistenceId = PersistenceId(entityId),
    emptyState =  VegState(None),
    commandHandler,
    eventHandler)

  private val commandHandler: (VegState, Command) => Effect[VegEvent, VegState] = { (state, command) =>
    command match {
      case x: EntityCreateVeg =>
        val id = x.id
        val entity = Veg(id, x.create.data)
        val created = VegCreated(entity)
        Effect.persist(created).thenRun(_ => x.replyTo.tell(created))

      case x: EntityGetVeg =>
        state.entity.map(x.replyTo.tell)
        Effect.none

      case x: EntityGetState =>
        x.replyTo.tell(state)
        Effect.none

      case x: EntityModifyState =>
        val compensatingActionPerformed = VegCompensatingActionPerformed(x.state)
        Effect.persist(compensatingActionPerformed).thenRun(_ => x.replyTo.tell(compensatingActionPerformed))

      case _ => Effect.unhandled
    }
  }

  private val eventHandler: (VegState, VegEvent) => VegState = { (state, event) =>
    state match {
      case state: VegState =>
        event match {
        case VegCreated(module) =>
          VegState(Some(module))
        case VegCompensatingActionPerformed(newState) =>
          newState
        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
      }
      case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
    }
  }

}


class VegEntityDatabase(system: ActorSystem[_], val producer: Publisher)(implicit val ex: ExecutionContext)
  extends VegDatabase{
  import akka.util.Timeout
  import scala.concurrent.duration._
  import akka.actor.typed.scaladsl.adapter._
  private implicit val askTimeout: Timeout = Timeout(5.seconds)

  override def typeKey = VegEntity.entityTypeKey
  val sharding =  ClusterSharding(system)
  val psEntities: ActorRef[ShardingEnvelope[VegEntity.Command]] =
    sharding.init(Entity(typeKey = typeKey,
      createBehavior = createEntity(VegEntity.behavior)(system.toUntyped))
      .withSettings(ClusterShardingSettings(system)))

  def entity(id: String) =
    sharding.entityRefFor(VegEntity.entityTypeKey, id)

  override def createVeg(x: CreateVeg): Future[VegCreated] = {
    val id = UUID.randomUUID()
    entity(id.toString) ? VegEntity.EntityCreateVeg(id, x)
  }

  override def getVeg(x: GetVeg): Future[Veg] =
    entity(x.id.toString) ? VegEntity.EntityGetVeg(x)

  override def getState(id: String): Future[VegState] =
    entity(id) ? VegEntity.EntityGetState(UUID.fromString(id))

  override def modifyState(id: String, state: VegState): Future[VegState] =
    (entity(id) ? VegEntity.EntityModifyState(state)).map(_.state)
}
