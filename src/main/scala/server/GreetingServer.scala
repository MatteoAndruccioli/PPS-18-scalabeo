package server

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import server.GameServerToGreeting.EndGameToGreeting
import server.GreetingToGameServer.{EndGameToGreetingAck, InitGame}
import shared.ClientToGreetingMessages.{ConnectionToGreetingQuery, DisconnectionToGreetingNotification, PlayerReadyAnswer}
import shared.Topic.GREETING_SERVER_RECEIVES_TOPIC
import shared.GreetingToClientMessages.{ConnectionAnswer, DisconnectionAck, ReadyToJoinAck, ReadyToJoinQuery}

import scala.collection.mutable

class GreetingServer extends Actor {

  private val mediator = DistributedPubSub.get(context.system).mediator
  private val cluster = Cluster.get(context.system)
  private val isServerOn = true

  private val nPlayer = 4

  private var listPlayers = new mutable.ListBuffer[ActorRef]()
  private var mapPlayersName = mutable.Map[ActorRef, String]()
  private var readyPlayers = new mutable.Queue[ActorRef]()

  var games = mutable.Map[ActorRef, List[ActorRef]]()
  var gameNumber = 0

  //server si sottoscrive al proprio topic
  mediator ! Subscribe(GREETING_SERVER_RECEIVES_TOPIC, self)

  override def receive: Receive = {
    case message: ConnectionToGreetingQuery =>
      listPlayers += sender()
      mapPlayersName += (sender() -> message.username)
      sender ! ConnectionAnswer(isServerOn)
      if(listPlayers.size>=nPlayer){
        mediator ! Publish(GREETING_SERVER_RECEIVES_TOPIC, ReadyToJoinQuery())
      }
    case PlayerReadyAnswer(answer) =>
      sender ! ReadyToJoinAck()
      if(answer) {
        readyPlayers.enqueue(sender())
        if (readyPlayers.size >= nPlayer) {
          val playersForGame = List[ActorRef](readyPlayers.dequeue(), readyPlayers.dequeue(),readyPlayers.dequeue(),readyPlayers.dequeue())
          for (player <- playersForGame) listPlayers -= player
          val gameServer = context.actorOf(Props(new GameServer(playersForGame, mapPlayersName.filter(user => playersForGame.contains(user._1)).toMap)), "gameServer" + gameNumber)
          games += (gameServer -> playersForGame)
          gameNumber = gameNumber + 1
          gameServer ! InitGame()
        }
      } else {
          listPlayers-=sender()
          mapPlayersName -= sender()
      }

    //fine di una partita
    case _ : EndGameToGreeting =>
      sender() ! EndGameToGreetingAck()
      if(games.contains(sender())) {
        games.remove(sender())
      }
    //disconnessione di un giocatore
    case _ : DisconnectionToGreetingNotification =>
      sender() ! DisconnectionAck()
      if(listPlayers.contains(sender())) {
        listPlayers -= sender()
      }
  }
}

object GreetingServer{
  def props() = Props(new GreetingServer())
}

