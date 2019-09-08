package com.affine.Carrot

import java.util.UUID
import io.surfkit.typebus._
import io.surfkit.typebus.event.DbAccessor
import io.surfkit.typebus.entity.EntityDb
import scala.concurrent.Future

package object data {


  sealed trait CarrotCommand
  case class CreateCarrot(data: String) extends CarrotCommand
  case class GetCarrot(id: UUID) extends CarrotCommand
  case class GetCarrotEntityState(id: String) extends CarrotCommand with DbAccessor

  sealed trait CarrotEvent
  case class CarrotCreated(entity: Carrot) extends CarrotEvent
  case class CarrotCompensatingActionPerformed(state: CarrotState) extends CarrotEvent
  case class CarrotState(entity: Option[Carrot])
  case class Carrot(id: UUID, data: String)

  object Implicits extends AvroByteStreams{
    implicit val createCarrotRW = Typebus.declareType[CreateCarrot, AvroByteStreamReader[CreateCarrot], AvroByteStreamWriter[CreateCarrot]]
    implicit val CarrotCreatedRW = Typebus.declareType[CarrotCreated, AvroByteStreamReader[CarrotCreated], AvroByteStreamWriter[CarrotCreated]]
    implicit val CarrotRW = Typebus.declareType[Carrot, AvroByteStreamReader[Carrot], AvroByteStreamWriter[Carrot]]
    implicit val getCarrotRW = Typebus.declareType[GetCarrot, AvroByteStreamReader[GetCarrot], AvroByteStreamWriter[GetCarrot]]
    implicit val getCarrotEntityStateRW = Typebus.declareType[GetCarrotEntityState, AvroByteStreamReader[GetCarrotEntityState], AvroByteStreamWriter[GetCarrotEntityState]]
    implicit val CarrotStateRW = Typebus.declareType[CarrotState, AvroByteStreamReader[CarrotState], AvroByteStreamWriter[CarrotState]]
  }

  trait CarrotDatabase extends EntityDb[CarrotState]{
    def createCarrot(x: CreateCarrot): Future[CarrotCreated]
    def getCarrot(x: GetCarrot): Future[Carrot]
  }
}



