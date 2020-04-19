package client

import akka.actor.{Actor, ActorRef}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import shared.GameServerToClientMessages

/*
  con un TestProbe non sono in grado di utilizzare il mediator quindi creo questa semplice classe ai fini di test:

  l'idea è che il testProbe che impersona il gameServer invia messaggi a questo attore che poi a sua volta
  li inoltra sul topic del gameServer attraverso il mediator, in questo modo il Client leggerà i messaggi sul topic
  del gameServer e crederà che provengano dal gameServer

    un attore di questo tipo richiede due parametri:
      - un topic su cui scrivere
      - il valore dell'actorRef relativo all'attore che gli invia messaggi impersonificando il gameServer
 */
class OnGameTopicSenderActor(gameServerTopic: String, gameServer: ActorRef) extends Actor {

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(gameServerTopic, self)

  override def receive: Receive = {
    case msg : GameServerToClientMessages => {
      println("SENDER = " + sender + "  === GAMESERVER = " + gameServer + " GAME_SERVER_TOPIC = " + gameServerTopic)
      if(sender == gameServer) mediator ! Publish(gameServerTopic, msg)
    }
  }
}
