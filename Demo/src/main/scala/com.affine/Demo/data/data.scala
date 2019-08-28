package com.affine.Demo

import java.util.UUID
import io.surfkit.typebus._
import scala.concurrent.Future

package object data {


  sealed trait DemoCommand
  case class CreateDemo(data: String) extends DemoCommand
  case class GetDemo(id: UUID) extends DemoCommand

  sealed trait DemoEvent
  case class DemoCreated(entity: Demo) extends DemoEvent
  case class DemoState(entity: Option[Demo])
  case class Demo(id: UUID, data: String)

  object Implicits extends AvroByteStreams{
    implicit val createDemoRW = Typebus.declareType[CreateDemo, AvroByteStreamReader[CreateDemo], AvroByteStreamWriter[CreateDemo]]
    implicit val DemoCreatedRW = Typebus.declareType[DemoCreated, AvroByteStreamReader[DemoCreated], AvroByteStreamWriter[DemoCreated]]
    implicit val DemoRW = Typebus.declareType[Demo, AvroByteStreamReader[Demo], AvroByteStreamWriter[Demo]]
    implicit val getDemoRW = Typebus.declareType[GetDemo, AvroByteStreamReader[GetDemo], AvroByteStreamWriter[GetDemo]]
  }

  trait CQRSDatabase[S]{
    def getState(id: String): Future[S]
    // TODO compensating action
    //def modifyState(id: String, state: S):
  }

  trait DemoDatabase{
    def createDemo(x: CreateDemo): Future[DemoCreated]
    def getDemo(x: GetDemo): Future[Demo]
  }
}



