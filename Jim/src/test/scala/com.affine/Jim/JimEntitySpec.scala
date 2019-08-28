package com.affine.Jim

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.affine.Jim.data._
import com.affine.Jim.entity.JimEntity
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class JimEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll {

  private val system = ActorSystem("jim")

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    system.terminate
  }

  val userId = UUID.randomUUID()

  val userNotFound = UUID.randomUUID()


  val someJim = Jim(
    id = userId,
    data = "Some Name"
  )
/*
  "user entity" should {

    "create a user" in  {
      val outcome = driver.run(CreateUserCommand(someUser))
      val retUser = outcome.replies.head.asInstanceOf[User]
      //driver.getAllIssues should have size 0                // fixme: complains about java serialization being used
      assert( retUser == someUser )
    }

    "be able to get the new user user" in  {
      val outcome = driver.run(GetUserCommand(userId))
      val user = outcome.replies.head.asInstanceOf[User]
      assert( user == someUser )
    }

  }
  */
}
