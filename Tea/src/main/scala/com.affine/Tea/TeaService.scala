package com.affine.Tea

import akka.actor._
import com.affine.Tea.data._
import io.surfkit.typebus
import io.surfkit.typebus._
import io.surfkit.typebus.annotations.ServiceMethod
import io.surfkit.typebus.bus.{Publisher, RetryBackoff}
import io.surfkit.typebus.event.{EventMeta, ServiceIdentifier}
import io.surfkit.typebus.module.Service

import scala.concurrent.Future
import scala.concurrent.duration._

class TeaService(serviceIdentifier: ServiceIdentifier, publisher: Publisher, sys: ActorSystem, teaDb: TeaDatabase) extends Service(serviceIdentifier,publisher) with AvroByteStreams{
  implicit val system = sys

  system.log.info("Starting service: " + serviceIdentifier.name)
  val bus = publisher.busActor
  import com.affine.Tea.data.Implicits._

  @ServiceMethod
  def createTea(createTea: CreateTea, meta: EventMeta): Future[TeaCreated] = teaDb.createTea(createTea)
  registerStream(createTea _)
    .withPartitionKey(_.entity.id.toString)

  @ServiceMethod
  def getTea(getTea: GetTea, meta: EventMeta): Future[Tea] = teaDb.getTea(getTea)
  registerStream(getTea _)
    .withRetryPolicy{
    case _ => typebus.bus.RetryPolicy(3, 1 second, RetryBackoff.Exponential)
  }

  system.log.info("Finished registering streams, trying to start service.")

}