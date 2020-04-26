package client

import akka.actor.{ActorSystem, Props}
import client.controller.Controller
import client.controller.ControllerLogic.CleverLogic
import com.typesafe.config.ConfigFactory

/** Permette di lanciare ClientActor su una porta libera */
object ClientLauncher extends App{
  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 0)
    .withFallback(ConfigFactory.parseString("akka.cluster.roles = [clientRole]"))
    .withFallback(ConfigFactory.load())

  val system = ActorSystem.create("ClusterSystem", config)

  val client = system.actorOf(Props.create(classOf[ClientActor]), "client")

  Controller.init(client, CleverLogic())
}
