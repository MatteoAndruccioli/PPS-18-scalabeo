package client

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

//launcher del client
object ClientLauncher extends App{
  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 0)
    .withFallback(ConfigFactory.parseString("akka.cluster.roles = [clientRole]"))
    .withFallback(ConfigFactory.load())

  val system = ActorSystem.create("ClusterSystem", config)

  system.actorOf(Props.create(classOf[ClientActor]), "client")
}
