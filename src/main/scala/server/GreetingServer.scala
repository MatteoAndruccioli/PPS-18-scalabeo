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

import scala.collection.immutable.Queue


class GreetingServer extends Actor {

  private val mediator = DistributedPubSub.get(context.system).mediator
  private val cluster = Cluster.get(context.system)
  private val isServerOn = true

  private val nPlayer = 4

  private var listPlayers = List[ActorRef]()
  private var mapPlayersName = Map[ActorRef, String]()
  private var readyPlayers = Queue[ActorRef]()

  var games = Map[ActorRef, List[ActorRef]]()
  var gameNumber = 0

  //server si sottoscrive al proprio topic
  mediator ! Subscribe(GREETING_SERVER_RECEIVES_TOPIC, self)

  override def receive: Receive = {
    case message: ConnectionToGreetingQuery =>
      listPlayers = List.concat(listPlayers,List(sender()))
      mapPlayersName += (sender() -> message.username)
      sender ! ConnectionAnswer(isServerOn)
      if(listPlayers.size>=nPlayer){
        mediator ! Publish(GREETING_SERVER_RECEIVES_TOPIC, ReadyToJoinQuery())
      }
    case PlayerReadyAnswer(answer) =>
      sender ! ReadyToJoinAck()
      if(answer) {
        readyPlayers = readyPlayers.enqueue(sender())
        if (readyPlayers.size >= nPlayer) {
          val playersForGame : List[ActorRef] = List.fill[ActorRef](nPlayer)({
            val player =readyPlayers.dequeue._1
            readyPlayers = readyPlayers.dequeue._2
            player
          })
          println("!!!!!!!!!!SERVER: I GIOCATORI CHE GIOCHERANNO SARANNO: "+playersForGame.toString())
          listPlayers = listPlayers.filterNot(playersForGame.contains)
          val gameServer = context.actorOf(Props(new GameServer(playersForGame, mapPlayersName.filter(user => playersForGame.contains(user._1)))), "gameServer" + gameNumber)
          games += (gameServer -> playersForGame)
          gameNumber = gameNumber + 1
          gameServer ! InitGame()
        }
      } else {
          listPlayers=listPlayers.filter( _ != sender())
          mapPlayersName -= sender()
      }

    //fine di una partita
    case _ : EndGameToGreeting =>
      sender() ! EndGameToGreetingAck()
      if(games.contains(sender())) {
        games-=sender()
      }
    //disconnessione di un giocatore
    case _ : DisconnectionToGreetingNotification =>
      sender() ! DisconnectionAck()
      if(listPlayers.contains(sender())) {
        listPlayers = listPlayers.filter(_ != sender())
        mapPlayersName-=sender()
        if(readyPlayers.toSet.contains(sender())){
          readyPlayers = readyPlayers.filter(_ != sender())
        }
      }
  }
}

object GreetingServer{
  def props() = Props(new GreetingServer())
}

