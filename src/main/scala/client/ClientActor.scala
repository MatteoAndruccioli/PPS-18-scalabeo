package client

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, Props}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}

import scala.concurrent.duration.Duration
import shared.Topic.{CLIENT_TOPIC, SERVER_TOPIC}
import shared.DemoMessage.{ClientMessage, ServerMessage}

class ClientActor extends Actor{
  private val mediator = DistributedPubSub.get(context.system).mediator
  private val cluster = Cluster.get(context.system)
  var counter:Int = 0

  //faccio si che il Client si sottoscriva al proprio topic
  mediator ! Subscribe(CLIENT_TOPIC, self)

  //avvio lo scheduler
  initUpdate()

  //ogni volta che il client riceve un messaggio dal server lo stampa
  override def receive: Receive = {
    case received: ServerMessage =>
      println("Client " + self + " - ho ricevuto messaggio " + received.message + " dal Server " + sender() )
  }


  //il client invia continuamente messaggi attraverso uno scheduler
  private def initUpdate(): Unit = {
    val ec = cluster.system.dispatcher
    val interval = Duration.create(1, TimeUnit.SECONDS)
    cluster.system.scheduler.schedule(interval, interval, () => sendOnTopic)(ec)
  }

  //invio di un ClientMessage numerato sul topic del server
  private def sendOnTopic(): Unit = {
    counter += 1
    mediator ! Publish(SERVER_TOPIC, ClientMessage("messaggio n. " + counter))
  }
}

object ClientActor{
  def props() = Props(classOf[ClientActor])
}
