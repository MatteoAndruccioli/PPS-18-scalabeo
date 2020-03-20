package server

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import shared.{ClusterScheduler, CustomScheduler}
import shared.Topic.{CLIENT_TOPIC, SERVER_TOPIC}
import shared.DemoMessage._

//il server ha il comportamento contrario al client:
//aspetta di esser contattato => invia un ack => invia messaggi al client fino a ricezione dell'ack
class ServerActor extends Actor{
  private val mediator = DistributedPubSub.get(context.system).mediator
  private val cluster = Cluster.get(context.system)
  private val scheduler: CustomScheduler = ClusterScheduler(cluster)

  var counter:Int = 0
  private var client: Option[ActorRef] = None

  //server si sottoscrive al proprio topic
  mediator ! Subscribe(SERVER_TOPIC, self)

  override def receive: Receive = waitingClientMediatorMessage

  //quando riceve un ClientMessage invia un messaggio ServerMessage
  //con stesso contenuto ma in maiuscolo
  def waitingClientMediatorMessage: Receive = {
    case received: ClientMediatorMessage =>
      client = Some(sender())
      println("Server " + self + " - ho ricevuto messaggio " + received.message + " dal Client " + sender() )
      client.get ! ServerAck()
      scheduler.replaceBehaviourAndStart(()=>sendOnTopic)//avvio lo scheduler
      context.become(waitingClientAck)
  }

  def waitingClientAck: Receive = {
    case _: ClientAck =>
      println("Server " + self + " - ho ricevuto messaggio Ack dal Client " + sender() )
      scheduler.stopTask()
  }

  //invio di un ClientMessage numerato sul topic del server
  private def sendOnTopic(): Unit = {
    counter += 1
    println("Server manda messaggio n° " + counter)
    mediator ! Publish(CLIENT_TOPIC, ServerMediatorMessage("messaggio n. " + counter))
  }
}

object ServerActor{
  def props() = Props(classOf[ServerActor])
}

