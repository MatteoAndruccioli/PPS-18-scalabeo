package server

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory



/** Permette di lanciare il server sulla porta specificata anche in application.conf (2551)
 */
object ServerLauncher extends App{
  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2551)
    .withFallback(ConfigFactory.parseString("akka.cluster.roles = [serverRole]"))
    .withFallback(ConfigFactory.load())

  val system = ActorSystem.create("ClusterSystem", config)

  system.actorOf(Props.create(classOf[GreetingServer]), "server")
}