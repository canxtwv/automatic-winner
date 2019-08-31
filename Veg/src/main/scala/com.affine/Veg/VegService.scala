package com.affine.Veg

import akka.actor._
import com.affine.Veg.data._
import io.surfkit.typebus
import io.surfkit.typebus._
import io.surfkit.typebus.annotations.ServiceMethod
import io.surfkit.typebus.bus.{Publisher, RetryBackoff}
import io.surfkit.typebus.event.{EventMeta, ServiceIdentifier}
import io.surfkit.typebus.module.Service

import scala.concurrent.Future
import scala.concurrent.duration._

class VegService(serviceIdentifier: ServiceIdentifier, publisher: Publisher, sys: ActorSystem, vegDb: VegDatabase) extends Service(serviceIdentifier,publisher) with AvroByteStreams{
  implicit val system = sys

  system.log.info("Starting service: " + serviceIdentifier.name)
  val bus = publisher.busActor
  import com.affine.Veg.data.Implicits._

  @ServiceMethod
  def createVeg(createVeg: CreateVeg, meta: EventMeta): Future[VegCreated] = vegDb.createVeg(createVeg)
  registerStream(createVeg _)
    .withPartitionKey(_.entity.id.toString)

  @ServiceMethod
  def getVeg(getVeg: GetVeg, meta: EventMeta): Future[Veg] = vegDb.getVeg(getVeg)
  registerStream(getVeg _)
    .withRetryPolicy{
    case _ => typebus.bus.RetryPolicy(3, 1 second, RetryBackoff.Exponential)
  }

  system.log.info("Finished registering streams, trying to start service.")

}