package server

import akka.actor.{Actor, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import shared.Topic.{CLIENT_TOPIC, SERVER_TOPIC}
import shared.DemoMessage.{ClientMessage, ServerMessage}

//server completamente reattivo: attende un messaggio sul proprio topic
//risponde sul topic del client inviando messaggio con testo in maiuscolo
class ServerActor extends Actor{
  val mediator = DistributedPubSub(context.system).mediator

  //server si sottoscrive al proprio topic
  mediator ! Subscribe(SERVER_TOPIC, self)

  //quando riceve un ClientMessage invia un messaggio ServerMessage
  //con stesso contenuto ma in maiuscolo
  override def receive: Receive = {
    case received: ClientMessage =>
      println("Server " + self + " - ho ricevuto messaggio " + received.message + " dal Client " + sender() )
      mediator ! Publish(CLIENT_TOPIC, ServerMessage(received.message.toUpperCase))
  }
}

object ServerActor{
  def props() = Props(classOf[ServerActor])
}

