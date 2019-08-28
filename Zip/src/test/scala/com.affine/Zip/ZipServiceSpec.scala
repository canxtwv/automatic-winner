package com.affine.zip

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.affine.Zip.data._
import io.surfkit.typebus.bus.TypebusApplication
import io.surfkit.typebus.bus.testkit._
import io.surfkit.typebus.client.Client
import io.surfkit.typebus.event.{EventMeta, PublishedEvent, ServiceIdentifier, ServiceException}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Either

class ZipServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  /*
  import com.affine.Zip.data.Implicits._
  implicit val system = ActorSystem("zip")
  implicit val actorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))
  val userDb = new UserDatabase{
    var state = Map.empty[UUID, User]

    def createUser(x: CreateUserCommand): Future[User] = {
      state += x.user.id -> x.user
      Future.successful(x.user)
    }
    def getUser(x: GetUserCommand): Future[User] =
      state.get(x.id).map(Future.successful).getOrElse(Future.failed(new RuntimeException("Can't find that user")))
  }

  lazy val serviceIdentity = ServiceIdentifier("zip")

  lazy val producer = new TypebusTestProducer(serviceIdentity, system)
  //lazy val service = new ZipService(serviceIdentity, producer, system, userDb)

  object service extends ZipService(serviceIdentity, producer, system, userDb){
    // you can mock your own "external" service responses that can target your RPC client
    def handleUser(u: User, meta: EventMeta): Future[Unit] = {
      meta.directReply.foreach{ rpc =>
        system.actorSelection(rpc.path).resolveOne(5 seconds).foreach{ a =>
          a ! PublishedEvent(meta, userRW.write(u) )
        }
      }
      Future.successful(Unit)
    }
    registerStream(handleUser _)
  }
  lazy val consumer = new TypebusTestConsumer(service, producer, system)

  TypebusApplication
  (
    system,
    serviceIdentity,
    producer,
    service,
    consumer
  )

  class ZipClient extends Client(serviceIdentity, producer, system){
    def createUser(x: CreateUserCommand): Future[Either[ServiceException, User]] = wire[CreateUserCommand, User](x)
    def getUser(x: GetUserCommand): Future[Either[ServiceException, User]] = wire[GetUserCommand, User](x)
  }

  val client = new ZipClient

  override protected def afterAll(): Unit = {
    system.terminate
  }

  val testUser = User(UUID.randomUUID(), "Test User")

  "Zip service " should {

    "create a user" in {
      for{
        u <-client.createUser(CreateUserCommand(testUser))
      }yield assert( u == Right(testUser) )
    }

  }
  */
}