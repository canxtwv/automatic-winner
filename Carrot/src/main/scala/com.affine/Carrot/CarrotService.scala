package com.affine.Carrot

import akka.actor._
import com.affine.Carrot.data._
import io.surfkit.typebus
import io.surfkit.typebus._
import io.surfkit.typebus.annotations.ServiceMethod
import io.surfkit.typebus.bus.{Publisher, RetryBackoff}
import io.surfkit.typebus.event.{EventMeta, ServiceIdentifier}
import io.surfkit.typebus.module.Service

import scala.concurrent.Future
import scala.concurrent.duration._

class CarrotService(serviceIdentifier: ServiceIdentifier, publisher: Publisher, sys: ActorSystem, carrotDb: CarrotDatabase) extends Service(serviceIdentifier,publisher) with AvroByteStreams{
  implicit val system = sys

  system.log.info("Starting service: " + serviceIdentifier.name)
  val bus = publisher.busActor
  import com.affine.Carrot.data.Implicits._

  registerDataBaseStream[GetCarrotEntityState, CarrotState](carrotDb)

  @ServiceMethod
  def createCarrot(createCarrot: CreateCarrot, meta: EventMeta): Future[CarrotCreated] = carrotDb.createCarrot(createCarrot)
  registerStream(createCarrot _)
    .withPartitionKey(_.entity.id.toString)

  @ServiceMethod
  def getCarrot(getCarrot: GetCarrot, meta: EventMeta): Future[Carrot] = carrotDb.getCarrot(getCarrot)
  registerStream(getCarrot _)
    .withRetryPolicy{
    case _ => typebus.bus.RetryPolicy(3, 1 second, RetryBackoff.Exponential)
  }

  system.log.info("Finished registering streams, trying to start service.")

}