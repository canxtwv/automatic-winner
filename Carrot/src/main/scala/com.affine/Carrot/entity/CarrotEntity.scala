package com.affine.Carrot.entity

import java.time.LocalDateTime
import java.util.UUID
import com.affine.Carrot.data._
import scala.concurrent.Future

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import io.surfkit.typebus.bus.Publisher
import io.surfkit.typebus.entity.EntityDb

object CarrotEntity {

  sealed trait Command
  // command
  final case class EntityCreateCarrot(id: UUID, create: CreateCarrot)(val replyTo: ActorRef[CarrotCreated]) extends Command
  final case class EntityModifyState(state: CarrotState)(val replyTo: ActorRef[CarrotCompensatingActionPerformed]) extends Command
  // query
  final case class EntityGetCarrot(get: GetCarrot)(val replyTo: ActorRef[Carrot]) extends Command
  final case class EntityGetState(id: UUID)(val replyTo: ActorRef[CarrotState]) extends Command

  val entityTypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("CarrotEntity")

  def behavior(entityId: String): Behavior[Command] =
    EventSourcedBehavior[Command, CarrotEvent, CarrotState](
    persistenceId = PersistenceId(entityId),
    emptyState =  CarrotState(None),
    commandHandler,
    eventHandler)

  private val commandHandler: (CarrotState, Command) => Effect[CarrotEvent, CarrotState] = { (state, command) =>
    command match {
      case x: EntityCreateCarrot =>
        val id = x.id
        val entity = Carrot(id, x.create.data)
        val created = CarrotCreated(entity)
        Effect.persist(created).thenRun(_ => x.replyTo.tell(created))

      case x: EntityGetCarrot =>
        state.entity.map(x.replyTo.tell)
        Effect.none

      case x: EntityGetState =>
        x.replyTo.tell(state)
        Effect.none

      case x: EntityModifyState =>
        val compensatingActionPerformed = CarrotCompensatingActionPerformed(x.state)
        Effect.persist(compensatingActionPerformed).thenRun(_ => x.replyTo.tell(compensatingActionPerformed))

      case _ => Effect.unhandled
    }
  }

  private val eventHandler: (CarrotState, CarrotEvent) => CarrotState = { (state, event) =>
    state match {
      case state: CarrotState =>
        event match {
        case CarrotCreated(module) =>
          CarrotState(Some(module))
        case CarrotCompensatingActionPerformed(newState) =>
          newState
        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
      }
      case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
    }
  }

}


class CarrotEntityDatabase(system: ActorSystem[_], val producer: Publisher)(implicit val ex: ExecutionContext)
  extends CarrotDatabase{
  import akka.util.Timeout
  import scala.concurrent.duration._
  import akka.actor.typed.scaladsl.adapter._
  private implicit val askTimeout: Timeout = Timeout(5.seconds)

  override def typeKey = CarrotEntity.entityTypeKey
  val sharding =  ClusterSharding(system)
  val psEntities: ActorRef[ShardingEnvelope[CarrotEntity.Command]] =
    sharding.init(Entity(typeKey = typeKey,
      createBehavior = createEntity(CarrotEntity.behavior)(system.toUntyped))
      .withSettings(ClusterShardingSettings(system)))

  def entity(id: String) =
    sharding.entityRefFor(CarrotEntity.entityTypeKey, id)

  override def createCarrot(x: CreateCarrot): Future[CarrotCreated] = {
    val id = UUID.randomUUID()
    entity(id.toString) ? CarrotEntity.EntityCreateCarrot(id, x)
  }

  override def getCarrot(x: GetCarrot): Future[Carrot] =
    entity(x.id.toString) ? CarrotEntity.EntityGetCarrot(x)

  override def getState(id: String): Future[CarrotState] =
    entity(id) ? CarrotEntity.EntityGetState(UUID.fromString(id))

  override def modifyState(id: String, state: CarrotState): Future[CarrotState] =
    (entity(id) ? CarrotEntity.EntityModifyState(state)).map(_.state)
}
