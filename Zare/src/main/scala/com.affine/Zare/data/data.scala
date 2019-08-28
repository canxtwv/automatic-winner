package com.affine.Zare

import java.util.UUID
import io.surfkit.typebus._
import scala.concurrent.Future

package object data {


  sealed trait ZareCommand
  case class CreateZare(data: String) extends ZareCommand
  case class GetZare(id: UUID) extends ZareCommand

  sealed trait ZareEvent
  case class ZareCreated(entity: Zare) extends ZareEvent
  case class ZareState(entity: Option[Zare])
  case class Zare(id: UUID, data: String)

  object Implicits extends AvroByteStreams{
    implicit val createZareRW = Typebus.declareType[CreateZare, AvroByteStreamReader[CreateZare], AvroByteStreamWriter[CreateZare]]
    implicit val ZareCreatedRW = Typebus.declareType[ZareCreated, AvroByteStreamReader[ZareCreated], AvroByteStreamWriter[ZareCreated]]
    implicit val ZareRW = Typebus.declareType[Zare, AvroByteStreamReader[Zare], AvroByteStreamWriter[Zare]]
    implicit val getZareRW = Typebus.declareType[GetZare, AvroByteStreamReader[GetZare], AvroByteStreamWriter[GetZare]]
  }

  trait CQRSDatabase[S]{
    def getState(id: String): Future[S]
    // TODO compensating action
    //def modifyState(id: String, state: S):
  }

  trait ZareDatabase{
    def createZare(x: CreateZare): Future[ZareCreated]
    def getZare(x: GetZare): Future[Zare]
  }
}



