package server

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import shared.ClientToGreetingMessages.{ConnectionToGreetingQuery, PlayerReadyAnswer}
import shared.Topic.GREETING_SERVER_RECEIVES_TOPIC
import shared.GreetingToClientMessages.{ConnectionAnswer, ReadyToJoinAck, ReadyToJoinQuery, StartGame}

import scala.collection.mutable

class GreetingServer extends Actor {

  private val mediator = DistributedPubSub.get(context.system).mediator
  private val cluster = Cluster.get(context.system)
  private val isServerOn = true

  private val nPlayer = 2

  private var listPlayers = new mutable.ListBuffer[ActorRef]()
  private var mapPlayersName = mutable.Map[ActorRef, String]()
  private var readyPlayers = new mutable.Queue[ActorRef]()

  //server si sottoscrive al proprio topic
  mediator ! Subscribe(GREETING_SERVER_RECEIVES_TOPIC, self)

  override def receive: Receive = {
    case message: ConnectionToGreetingQuery =>
      listPlayers += sender()
      println("MI HA CONTATTATO IL PLAYER " + message.username)
      mapPlayersName += (sender() -> message.username)
      sender ! ConnectionAnswer(isServerOn)
      if(listPlayers.size>=nPlayer){
        println("CI SONO ALMENO "+nPlayer+" giocatori")
        mediator ! Publish(GREETING_SERVER_RECEIVES_TOPIC, ReadyToJoinQuery())
      }
    case PlayerReadyAnswer(answer) =>
      sender ! ReadyToJoinAck()
      if(answer) {
        readyPlayers.enqueue(sender())
        if(readyPlayers.size>=nPlayer) {
          val playersForGame = List[ActorRef](readyPlayers.dequeue(),readyPlayers.dequeue()/*,readyPlayers.dequeue(),readyPlayers.dequeue()*/)
          for (player <- playersForGame) listPlayers-=player
          println("ListPlayers after the game start: " +listPlayers)
          // TODO Qua instanzierò il server che gestirà il gioco del quale conservero l' ActorRef e i quello dei partecipanti
          for(player <- playersForGame) player ! StartGame()
        } else {
          listPlayers-=sender()
          mapPlayersName -= sender()
        }
      }
  }
}

object GreetingServer{
  def props() = Props(new GreetingServer())
}

