package com.affine.Zip

import akka.actor._
import com.affine.Zip.data._
import io.surfkit.typebus
import io.surfkit.typebus._
import io.surfkit.typebus.annotations.ServiceMethod
import io.surfkit.typebus.bus.{Publisher, RetryBackoff}
import io.surfkit.typebus.event.{EventMeta, ServiceIdentifier}
import io.surfkit.typebus.module.Service

import scala.concurrent.Future
import scala.concurrent.duration._

class ZipService(serviceIdentifier: ServiceIdentifier, publisher: Publisher, sys: ActorSystem, zipDb: ZipDatabase) extends Service(serviceIdentifier,publisher) with AvroByteStreams{
  implicit val system = sys

  system.log.info("Starting service: " + serviceIdentifier.name)
  val bus = publisher.busActor
  import com.affine.Zip.data.Implicits._

  @ServiceMethod
  def createZip(createZip: CreateZip, meta: EventMeta): Future[ZipCreated] = zipDb.createZip(createZip)
  registerStream(createZip _)
    .withPartitionKey(_.entity.id.toString)

  @ServiceMethod
  def getZip(getZip: GetZip, meta: EventMeta): Future[Zip] = zipDb.getZip(getZip)
  registerStream(getZip _)
    .withRetryPolicy{
    case _ => typebus.bus.RetryPolicy(3, 1 second, RetryBackoff.Exponential)
  }

  system.log.info("Finished registering streams, trying to start service.")

}