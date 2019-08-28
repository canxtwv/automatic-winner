package com.affine.Zip

import java.util.UUID
import io.surfkit.typebus._
import scala.concurrent.Future

package object data {


  sealed trait ZipCommand
  case class CreateZip(data: String) extends ZipCommand
  case class GetZip(id: UUID) extends ZipCommand

  sealed trait ZipEvent
  case class ZipCreated(entity: Zip) extends ZipEvent
  case class ZipState(entity: Option[Zip])
  case class Zip(id: UUID, data: String)

  object Implicits extends AvroByteStreams{
    implicit val createZipRW = Typebus.declareType[CreateZip, AvroByteStreamReader[CreateZip], AvroByteStreamWriter[CreateZip]]
    implicit val ZipCreatedRW = Typebus.declareType[ZipCreated, AvroByteStreamReader[ZipCreated], AvroByteStreamWriter[ZipCreated]]
    implicit val ZipRW = Typebus.declareType[Zip, AvroByteStreamReader[Zip], AvroByteStreamWriter[Zip]]
    implicit val getZipRW = Typebus.declareType[GetZip, AvroByteStreamReader[GetZip], AvroByteStreamWriter[GetZip]]
  }

  trait CQRSDatabase[S]{
    def getState(id: String): Future[S]
    // TODO compensating action
    //def modifyState(id: String, state: S):
  }

  trait ZipDatabase{
    def createZip(x: CreateZip): Future[ZipCreated]
    def getZip(x: GetZip): Future[Zip]
  }
}



