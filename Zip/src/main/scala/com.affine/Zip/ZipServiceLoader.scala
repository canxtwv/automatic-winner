package com.affine.Zip

import akka.NotUsed
import akka.actor.typed.{ActorSystem, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import com.softwaremill.macwire._
import com.affine.Zip.entity._
import com.affine.Zip.entity.ZipEntityDatabase
import io.surfkit.typebus.bus.TypebusApplication
import io.surfkit.typebus.bus.kafka.{TypebusKafkaConsumer, TypebusKafkaProducer}
import io.surfkit.typebus.event.ServiceIdentifier


object ZipServiceLoader extends App{

  val k8sBehavior : Behavior[NotUsed] =
    Behaviors.setup { context =>

      implicit val ec = context.executionContext
      val untypedSystem = context.system.toUntyped
      // https://doc.akka.io/docs/akka-management/current/bootstrap/index.html
      // Akka Management hosts the HTTP routes used by bootstrap
      AkkaManagement(untypedSystem).start()
      // Starting the bootstrap process needs to be done explicitly
      ClusterBootstrap(untypedSystem).start()

      lazy val serviceIdentity = ServiceIdentifier("zip")

      // only want to activate and join cluster in certain cases
      //ZookeeperClusterSeed(system).join()
      lazy val producer = new TypebusKafkaProducer(serviceIdentity, untypedSystem)
      lazy val service = new ZipService(serviceIdentity, producer, untypedSystem, new ZipEntityDatabase(context.system))
      lazy val consumer = new TypebusKafkaConsumer(service, producer, untypedSystem)

      TypebusApplication
      (
        untypedSystem,
        serviceIdentity,
        producer,
        service,
        consumer
      )

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          Behaviors.stopped
      }
    }

  // Create an Akka untyped system
  val system = akka.actor.ActorSystem("zip")
  val typedActor = system.spawn(k8sBehavior, "k8sBehavior")
  //val system = ActorSystem(k8sBehavior, "module", ConfigFactory.load())
  system.whenTerminated // remove compiler warnings

  //val system = ActorSystem("module")
  //system.actorOf(Props(new ModuleServiceLoader))
  Thread.currentThread().join()
}

