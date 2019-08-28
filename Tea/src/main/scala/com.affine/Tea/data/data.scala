package com.affine.Tea

import java.util.UUID
import io.surfkit.typebus._
import scala.concurrent.Future

package object data {


  sealed trait TeaCommand
  case class CreateTea(data: String) extends TeaCommand
  case class GetTea(id: UUID) extends TeaCommand

  sealed trait TeaEvent
  case class TeaCreated(entity: Tea) extends TeaEvent
  case class TeaState(entity: Option[Tea])
  case class Tea(id: UUID, data: String)

  object Implicits extends AvroByteStreams{
    implicit val createTeaRW = Typebus.declareType[CreateTea, AvroByteStreamReader[CreateTea], AvroByteStreamWriter[CreateTea]]
    implicit val TeaCreatedRW = Typebus.declareType[TeaCreated, AvroByteStreamReader[TeaCreated], AvroByteStreamWriter[TeaCreated]]
    implicit val TeaRW = Typebus.declareType[Tea, AvroByteStreamReader[Tea], AvroByteStreamWriter[Tea]]
    implicit val getTeaRW = Typebus.declareType[GetTea, AvroByteStreamReader[GetTea], AvroByteStreamWriter[GetTea]]
  }

  trait CQRSDatabase[S]{
    def getState(id: String): Future[S]
    // TODO compensating action
    //def modifyState(id: String, state: S):
  }

  trait TeaDatabase{
    def createTea(x: CreateTea): Future[TeaCreated]
    def getTea(x: GetTea): Future[Tea]
  }
}



