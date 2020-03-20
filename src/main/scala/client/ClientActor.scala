package client

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import shared.ClusterScheduler

import scala.concurrent.duration.Duration
import shared.Topic.{CLIENT_TOPIC, SERVER_TOPIC}
import shared.DemoMessage._

class ClientActor extends Actor{
  private val mediator = DistributedPubSub.get(context.system).mediator
  private val cluster = Cluster.get(context.system)
  var counter:Int = 0
  private val scheduler: ClusterScheduler = istantiateEmptyScheduler()
  private var server: Option[ActorRef] = None

  //faccio si che il Client si sottoscriva al proprio topic
  mediator ! Subscribe(CLIENT_TOPIC, self)

  //avvio lo scheduler
  initUpdate()

  override def receive: Receive = waitingServerAck

  //client manda messaggio ClientMediatorMessage("messaggio1") finchè non riceve un ack dal server
  //in questo modo è certo che sia arrivato, quando ack arriva smette di inviare
  def waitingServerAck: Receive = {
    case _: ServerAck =>{
      //dopo la ricezione del primo messaggio dal server posso sapere qual è il suo ActorRef per contattarlo direttamente
      server = Some(sender())
      println("Client " + self + " - ho ricevuto messaggio Ack dal Server " + sender() )
      scheduler.stopTask()
      context.become(waitingServerMediatorMessage)
    }
  }


  //quando il client riceve un messaggio dal server invia un ack e stampa il messaggio ricevuto
  def waitingServerMediatorMessage: Receive = {
    case received: ServerMediatorMessage =>
      //uso ActorRef server che ho ricavato dal primo contatto con il server, per testare che funzioni questa logica
      server.get ! ClientAck()
      println("Client " + self + " - ho ricevuto messaggio " + received.message + " dal Server " + sender() )
  }


  //il client invia continuamente messaggi attraverso uno scheduler
  private def initUpdate(): Unit = {
    val ec = cluster.system.dispatcher
    val interval = Duration.create(1, TimeUnit.SECONDS)
    scheduler.replaceBehaviourAndStart(()=>sendOnTopic)
  }

  //invio di un ClientMessage numerato sul topic del server
  private def sendOnTopic(): Unit = {
    counter += 1
    mediator ! Publish(SERVER_TOPIC, ClientMediatorMessage("messaggio n. " + counter))
  }


  // i due interval andranno cambiati probabilmente
  private def istantiateEmptyScheduler(): ClusterScheduler = new ClusterScheduler(3, TimeUnit.SECONDS, 3, TimeUnit.SECONDS, None,cluster)

}

object ClientActor{
  def props() = Props(classOf[ClientActor])
}
