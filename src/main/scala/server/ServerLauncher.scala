package server

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

//launcher del server
//nota: la porta che usa è importante => deve essere quella indicata nel file application.conf
object ServerLauncher extends App{
  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2551)
    .withFallback(ConfigFactory.parseString("akka.cluster.roles = [serverRole]"))
    .withFallback(ConfigFactory.load())

  val system = ActorSystem.create("ClusterSystem", config)

  system.actorOf(Props.create(classOf[ServerActor]), "server")
}