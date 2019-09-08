package com.affine.Taco.entity

import java.time.LocalDateTime
import java.util.UUID
import com.affine.Taco.data._
import scala.concurrent.Future

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import io.surfkit.typebus.bus.Publisher
import io.surfkit.typebus.entity.EntityDb

object TestEntity {

  sealed trait Command
  // command
  final case class EntityCreateTest(id: UUID, create: CreateTest)(val replyTo: ActorRef[TestCreated]) extends Command
  final case class EntityModifyState(state: TestState)(val replyTo: ActorRef[TestCompensatingActionPerformed]) extends Command
  // query
  final case class EntityGetTest(get: GetTest)(val replyTo: ActorRef[Test]) extends Command
  final case class EntityGetState(id: UUID)(val replyTo: ActorRef[TestState]) extends Command

  val entityTypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("TestEntity")

  def behavior(entityId: String): Behavior[Command] =
    EventSourcedBehavior[Command, TestEvent, TestState](
    persistenceId = PersistenceId(entityId),
    emptyState =  TestState(None),
    commandHandler,
    eventHandler)

  private val commandHandler: (TestState, Command) => Effect[TestEvent, TestState] = { (state, command) =>
    command match {
      case x: EntityCreateTest =>
        val id = x.id
        val entity = Test(id, x.create.data)
        val created = TestCreated(entity)
        Effect.persist(created).thenRun(_ => x.replyTo.tell(created))

      case x: EntityGetTest =>
        state.entity.map(x.replyTo.tell)
        Effect.none

      case x: EntityGetState =>
        x.replyTo.tell(state)
        Effect.none

      case x: EntityModifyState =>
        val compensatingActionPerformed = TestCompensatingActionPerformed(x.state)
        Effect.persist(compensatingActionPerformed).thenRun(_ => x.replyTo.tell(compensatingActionPerformed))

      case _ => Effect.unhandled
    }
  }

  private val eventHandler: (TestState, TestEvent) => TestState = { (state, event) =>
    state match {
      case state: TestState =>
        event match {
        case TestCreated(module) =>
          TestState(Some(module))
        case TestCompensatingActionPerformed(newState) =>
          newState
        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
      }
      case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
    }
  }

}


class TestEntityDatabase(system: ActorSystem[_], val producer: Publisher)(implicit val ex: ExecutionContext)
  extends TestDatabase{
  import akka.util.Timeout
  import scala.concurrent.duration._
  import akka.actor.typed.scaladsl.adapter._
  private implicit val askTimeout: Timeout = Timeout(5.seconds)

  override def typeKey = TestEntity.entityTypeKey
  val sharding =  ClusterSharding(system)
  val psEntities: ActorRef[ShardingEnvelope[TestEntity.Command]] =
    sharding.init(Entity(typeKey = typeKey,
      createBehavior = createEntity(TestEntity.behavior)(system.toUntyped))
      .withSettings(ClusterShardingSettings(system)))

  def entity(id: String) =
    sharding.entityRefFor(TestEntity.entityTypeKey, id)

  override def createTest(x: CreateTest): Future[TestCreated] = {
    val id = UUID.randomUUID()
    entity(id.toString) ? TestEntity.EntityCreateTest(id, x)
  }

  override def getTest(x: GetTest): Future[Test] =
    entity(x.id.toString) ? TestEntity.EntityGetTest(x)

  override def getState(id: String): Future[TestState] =
    entity(id) ? TestEntity.EntityGetState(UUID.fromString(id))

  override def modifyState(id: String, state: TestState): Future[TestState] =
    (entity(id) ? TestEntity.EntityModifyState(state)).map(_.state)
}
