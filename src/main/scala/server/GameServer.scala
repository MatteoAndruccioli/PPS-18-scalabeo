package server

import shared.Topic.GAME_SERVER_SEND_TOPIC
import server.GreetingToGameServer.InitGame
import akka.actor.{Actor, ActorRef}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import model.{LettersBagImpl, LettersHandImpl}
import shared.GameServerToClientMessages.MatchTopicListenQuery
import shared.{ClusterScheduler, CustomScheduler}

import scala.collection.mutable

class GameServer(players : List[ActorRef], mapUsername : Map[ActorRef, String]) extends Actor {

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(GAME_SERVER_SEND_TOPIC, self)
  private val cluster = Cluster.get(context.system)
  private val scheduler: CustomScheduler = ClusterScheduler(cluster)
  private val nPlayer = players.size

  private var greetingServerRef: ActorRef = _
  private val gamePlayers = players
  private val gamePlayersUsername: Map[ActorRef, String] = mapUsername

  private val board = ??? //TODO inserire il tabellone del model
  private val pouch = LettersBagImpl()
  private var playersHand = mutable.Map[ActorRef, LettersHandImpl]()
  //creo le mani
  gamePlayers.foreach(p => playersHand+=(p -> LettersHandImpl.apply(mutable.ArrayBuffer(pouch.takeRandomElementFromBagOfLetters(8).get : _*))))


  override def receive: Receive = {
    case _: InitGame =>
      scheduler.replaceBehaviourAndStart(() => sendTopic())
      greetingServerRef = sender()
  }

  //comportamento dello scheduler
  private def sendTopic(): Unit = {
    gamePlayers.foreach(player => player ! MatchTopicListenQuery(GAME_SERVER_SEND_TOPIC, playersHand(player)._hand))
  }
}