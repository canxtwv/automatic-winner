package com.affine.Demo

import akka.actor._
import com.affine.Demo.data._
import io.surfkit.typebus
import io.surfkit.typebus._
import io.surfkit.typebus.annotations.ServiceMethod
import io.surfkit.typebus.bus.{Publisher, RetryBackoff}
import io.surfkit.typebus.event.{EventMeta, ServiceIdentifier}
import io.surfkit.typebus.module.Service

import scala.concurrent.Future
import scala.concurrent.duration._

class DemoService(serviceIdentifier: ServiceIdentifier, publisher: Publisher, sys: ActorSystem, demoDb: DemoDatabase) extends Service(serviceIdentifier,publisher) with AvroByteStreams{
  implicit val system = sys

  system.log.info("Starting service: " + serviceIdentifier.name)
  val bus = publisher.busActor
  import com.affine.Demo.data.Implicits._

  @ServiceMethod
  def createDemo(createDemo: CreateDemo, meta: EventMeta): Future[DemoCreated] = demoDb.createDemo(createDemo)
  registerStream(createDemo _)
    .withPartitionKey(_.entity.id.toString)

  @ServiceMethod
  def getDemo(getDemo: GetDemo, meta: EventMeta): Future[Demo] = demoDb.getDemo(getDemo)
  registerStream(getDemo _)
    .withRetryPolicy{
    case _ => typebus.bus.RetryPolicy(3, 1 second, RetryBackoff.Exponential)
  }

  system.log.info("Finished registering streams, trying to start service.")

}