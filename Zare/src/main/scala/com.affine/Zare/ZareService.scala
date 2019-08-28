package com.affine.Zare

import akka.actor._
import com.affine.Zare.data._
import io.surfkit.typebus
import io.surfkit.typebus._
import io.surfkit.typebus.annotations.ServiceMethod
import io.surfkit.typebus.bus.{Publisher, RetryBackoff}
import io.surfkit.typebus.event.{EventMeta, ServiceIdentifier}
import io.surfkit.typebus.module.Service

import scala.concurrent.Future
import scala.concurrent.duration._

class ZareService(serviceIdentifier: ServiceIdentifier, publisher: Publisher, sys: ActorSystem, zareDb: ZareDatabase) extends Service(serviceIdentifier,publisher) with AvroByteStreams{
  implicit val system = sys

  system.log.info("Starting service: " + serviceIdentifier.name)
  val bus = publisher.busActor
  import com.affine.Zare.data.Implicits._

  @ServiceMethod
  def createZare(createZare: CreateZare, meta: EventMeta): Future[ZareCreated] = zareDb.createZare(createZare)
  registerStream(createZare _)
    .withPartitionKey(_.entity.id.toString)

  @ServiceMethod
  def getZare(getZare: GetZare, meta: EventMeta): Future[Zare] = zareDb.getZare(getZare)
  registerStream(getZare _)
    .withRetryPolicy{
    case _ => typebus.bus.RetryPolicy(3, 1 second, RetryBackoff.Exponential)
  }

  system.log.info("Finished registering streams, trying to start service.")

}