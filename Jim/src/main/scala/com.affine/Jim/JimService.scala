package com.affine.Jim

import akka.actor._
import com.affine.Jim.data._
import io.surfkit.typebus
import io.surfkit.typebus._
import io.surfkit.typebus.annotations.ServiceMethod
import io.surfkit.typebus.bus.{Publisher, RetryBackoff}
import io.surfkit.typebus.event.{EventMeta, ServiceIdentifier}
import io.surfkit.typebus.module.Service

import scala.concurrent.Future
import scala.concurrent.duration._

class JimService(serviceIdentifier: ServiceIdentifier, publisher: Publisher, sys: ActorSystem, jimDb: JimDatabase) extends Service(serviceIdentifier,publisher) with AvroByteStreams{
  implicit val system = sys

  system.log.info("Starting service: " + serviceIdentifier.name)
  val bus = publisher.busActor
  import com.affine.Jim.data.Implicits._

  @ServiceMethod
  def createJim(createJim: CreateJim, meta: EventMeta): Future[JimCreated] = jimDb.createJim(createJim)
  registerStream(createJim _)
    .withPartitionKey(_.entity.id.toString)

  @ServiceMethod
  def getJim(getJim: GetJim, meta: EventMeta): Future[Jim] = jimDb.getJim(getJim)
  registerStream(getJim _)
    .withRetryPolicy{
    case _ => typebus.bus.RetryPolicy(3, 1 second, RetryBackoff.Exponential)
  }

  system.log.info("Finished registering streams, trying to start service.")

}