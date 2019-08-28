package com.affine.Jim

import java.util.UUID
import io.surfkit.typebus._
import scala.concurrent.Future

package object data {


  sealed trait JimCommand
  case class CreateJim(data: String) extends JimCommand
  case class GetJim(id: UUID) extends JimCommand

  sealed trait JimEvent
  case class JimCreated(entity: Jim) extends JimEvent
  case class JimState(entity: Option[Jim])
  case class Jim(id: UUID, data: String)

  object Implicits extends AvroByteStreams{
    implicit val createJimRW = Typebus.declareType[CreateJim, AvroByteStreamReader[CreateJim], AvroByteStreamWriter[CreateJim]]
    implicit val JimCreatedRW = Typebus.declareType[JimCreated, AvroByteStreamReader[JimCreated], AvroByteStreamWriter[JimCreated]]
    implicit val JimRW = Typebus.declareType[Jim, AvroByteStreamReader[Jim], AvroByteStreamWriter[Jim]]
    implicit val getJimRW = Typebus.declareType[GetJim, AvroByteStreamReader[GetJim], AvroByteStreamWriter[GetJim]]
  }

  trait CQRSDatabase[S]{
    def getState(id: String): Future[S]
    // TODO compensating action
    //def modifyState(id: String, state: S):
  }

  trait JimDatabase{
    def createJim(x: CreateJim): Future[JimCreated]
    def getJim(x: GetJim): Future[Jim]
  }
}



