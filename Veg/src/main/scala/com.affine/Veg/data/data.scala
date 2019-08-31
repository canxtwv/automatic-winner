package com.affine.Veg

import java.util.UUID
import io.surfkit.typebus._
import io.surfkit.typebus.event.DbAccessor
import io.surfkit.typebus.entity.EntityDb
import scala.concurrent.Future

package object data {


  sealed trait VegCommand
  case class CreateVeg(data: String) extends VegCommand
  case class GetVeg(id: UUID) extends VegCommand
  case class GetVegEntityState(id: String) extends VegCommand with DbAccessor

  sealed trait VegEvent
  case class VegCreated(entity: Veg) extends VegEvent
  case class VegCompensatingActionPerformed(state: VegState) extends VegEvent
  case class VegState(entity: Option[Veg])
  case class Veg(id: UUID, data: String)

  object Implicits extends AvroByteStreams{
    implicit val createVegRW = Typebus.declareType[CreateVeg, AvroByteStreamReader[CreateVeg], AvroByteStreamWriter[CreateVeg]]
    implicit val VegCreatedRW = Typebus.declareType[VegCreated, AvroByteStreamReader[VegCreated], AvroByteStreamWriter[VegCreated]]
    implicit val VegRW = Typebus.declareType[Veg, AvroByteStreamReader[Veg], AvroByteStreamWriter[Veg]]
    implicit val getVegRW = Typebus.declareType[GetVeg, AvroByteStreamReader[GetVeg], AvroByteStreamWriter[GetVeg]]
  }

  trait VegDatabase extends EntityDb[VegState]{
    def createVeg(x: CreateVeg): Future[VegCreated]
    def getVeg(x: GetVeg): Future[Veg]
  }
}



