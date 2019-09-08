package com.affine.Taco

import akka.actor._
import com.affine.Taco.data._
import io.surfkit.typebus
import io.surfkit.typebus._
import io.surfkit.typebus.annotations.ServiceMethod
import io.surfkit.typebus.bus.{Publisher, RetryBackoff}
import io.surfkit.typebus.event.{EventMeta, ServiceIdentifier}
import io.surfkit.typebus.module.Service

import scala.concurrent.Future
import scala.concurrent.duration._

class TacoService(serviceIdentifier: ServiceIdentifier, publisher: Publisher, sys: ActorSystem, testDb: TestDatabase) extends Service(serviceIdentifier,publisher) with AvroByteStreams{
  implicit val system = sys

  system.log.info("Starting service: " + serviceIdentifier.name)
  val bus = publisher.busActor
  import com.affine.Taco.data.Implicits._

  @ServiceMethod
  def createTest(createTest: CreateTest, meta: EventMeta): Future[TestCreated] = testDb.createTest(createTest)
  registerStream(createTest _)
    .withPartitionKey(_.entity.id.toString)

  @ServiceMethod
  def getTest(getTest: GetTest, meta: EventMeta): Future[Test] = testDb.getTest(getTest)
  registerStream(getTest _)
    .withRetryPolicy{
    case _ => typebus.bus.RetryPolicy(3, 1 second, RetryBackoff.Exponential)
  }

  system.log.info("Finished registering streams, trying to start service.")

}