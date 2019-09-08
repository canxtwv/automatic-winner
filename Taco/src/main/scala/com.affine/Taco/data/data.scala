package com.affine.Taco

import java.util.UUID
import io.surfkit.typebus._
import io.surfkit.typebus.event.DbAccessor
import io.surfkit.typebus.entity.EntityDb
import scala.concurrent.Future

package object data {


  sealed trait TestCommand
  case class CreateTest(data: String) extends TestCommand
  case class GetTest(id: UUID) extends TestCommand
  case class GetTestEntityState(id: String) extends TestCommand with DbAccessor

  sealed trait TestEvent
  case class TestCreated(entity: Test) extends TestEvent
  case class TestCompensatingActionPerformed(state: TestState) extends TestEvent
  case class TestState(entity: Option[Test])
  case class Test(id: UUID, data: String)

  object Implicits extends AvroByteStreams{
    implicit val createTestRW = Typebus.declareType[CreateTest, AvroByteStreamReader[CreateTest], AvroByteStreamWriter[CreateTest]]
    implicit val TestCreatedRW = Typebus.declareType[TestCreated, AvroByteStreamReader[TestCreated], AvroByteStreamWriter[TestCreated]]
    implicit val TestRW = Typebus.declareType[Test, AvroByteStreamReader[Test], AvroByteStreamWriter[Test]]
    implicit val getTestRW = Typebus.declareType[GetTest, AvroByteStreamReader[GetTest], AvroByteStreamWriter[GetTest]]
  }

  trait TestDatabase extends EntityDb[TestState]{
    def createTest(x: CreateTest): Future[TestCreated]
    def getTest(x: GetTest): Future[Test]
  }
}



