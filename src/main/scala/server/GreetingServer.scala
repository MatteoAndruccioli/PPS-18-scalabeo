package server

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import server.GameServerToGreeting.EndGameToGreeting
import server.GreetingToGameServer.{EndGameToGreetingAck, InitGame}
import shared.ClientToGreetingMessages.{ConnectionToGreetingQuery, DisconnectionToGreetingNotification, PlayerReadyAnswer}
import shared.Channels.GREETING_SERVER_RECEIVES_TOPIC
import shared.GreetingToClientMessages.{ConnectionAnswer, DisconnectionAck, ReadyToJoinAck, ReadyToJoinQuery}

import scala.collection.immutable.Queue

/** GreetingServer è l'attore server che gestisce ogni fase di creazione e terminazione delle partite.
 * GreetingServer accoglie le richieste di ogni client che desidera utilizzare l'applicazione, crea una partita quando
 * riceve almeno 4 richieste e maniente le informazioni delle partite in corso.
 */
class GreetingServer extends Actor {

  private val mediator = DistributedPubSub.get(context.system).mediator
  private val cluster = Cluster.get(context.system)
  private val isServerOn = true

  private var gameNumber = 0
  private val nPlayer = 4

  private var listPlayers = List[ActorRef]()
  private var mapPlayersName = Map[ActorRef, String]()
  private var readyPlayers = Queue[ActorRef]()
  private var games = Map[ActorRef, List[ActorRef]]()

  mediator ! Subscribe(GREETING_SERVER_RECEIVES_TOPIC, self)

  /** Fornisce la definizione del comportamento unico del GreetingServer.
   *  Essendo un server questo deve poter rispondere ad ogni richiesta leggitima dei client durante la fase di pre/post
   *  partita, quali:
   *    - richiesta di voler utilizzare l'applicativo
   *    - richiesta di voler unirsi alla partita creata dal GreetingServer
   *    - gestione di una partita terminata
   *    - gestione della disconnessione
   */
  override def receive: Receive = {
    //desiderio di utilizzare il gioco
    case message: ConnectionToGreetingQuery =>
      listPlayers = List.concat(listPlayers,List(sender()))
      mapPlayersName += (sender() -> message.username)
      sender ! ConnectionAnswer(isServerOn)
      if(listPlayers.size>=nPlayer){
        mediator ! Publish(GREETING_SERVER_RECEIVES_TOPIC, ReadyToJoinQuery())
      }
    //risposta alla partita trovata
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

