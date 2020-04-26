package server

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import model.{BoardTileImpl, Card, CardImpl, DictionaryImpl, PositionImpl}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import server.GreetingToGameServer.InitGame
import shared.ClientMoveAckType.{PassAck, TimeoutAck}
import shared.ClientToGameServerMessages.{ClientMadeMove, EndTurnUpdateAck, MatchTopicListenAck, PlayerTurnBeginAck}
import shared.GameServerToClientMessages.{ClientMoveAck, EndTurnUpdate, MatchTopicListenQuery, PlayerTurnBegins}
import shared.Move.{Pass, TimeOut, WordMove}

import scala.concurrent.duration.FiniteDuration

class GameServerGameTest extends TestKit(ActorSystem("test-system"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll {

    override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val probe1 = TestProbe()
  val probe2 = TestProbe()
  val playerList = List(probe1.ref,probe2.ref)
  val mapUsername = Map(probe1.ref -> "Mike", probe2.ref -> "Lisa")
  val gameServer = system.actorOf(Props(new GameServer(playerList,mapUsername)), "g")
  var topic : String = _
  var handPlayer : Vector[Card] = _

  /*verifico l'inizializzazione corretta*/
  "GameServer" should {

    "accept a timeout Move" in {
      gameServer ! InitGame()
      topic = probe1.expectMsgType[MatchTopicListenQuery](new FiniteDuration(10, TimeUnit.SECONDS)).gameServerTopic
      handPlayer = probe2.expectMsgType[MatchTopicListenQuery](new FiniteDuration(10, TimeUnit.SECONDS)).playerHand
      probe1.send(gameServer, MatchTopicListenAck())
      probe2.send(gameServer, MatchTopicListenAck())
      /*verifico che non riceva alcun messaggio diretto postumo*/
      probe1.expectNoMessage(new FiniteDuration(10, TimeUnit.SECONDS))

      /*iscrivo ogni client al mediator*/
      val mediator = DistributedPubSub(system).mediator
      mediator ! Subscribe(topic, probe1.ref)
      mediator ! Subscribe(topic, probe2.ref)
      /*verifico l'ack della sottoscrizione*/
      expectMsgType[SubscribeAck]
      expectMsgType[SubscribeAck]

      /*verifico di ricevere il messaggio del turno iniziale*/
      probe1.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS), PlayerTurnBegins(probe1.ref))
      probe2.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS), PlayerTurnBegins(probe1.ref))
      probe1.send(gameServer, PlayerTurnBeginAck())
      probe2.send(gameServer, PlayerTurnBeginAck())

      /*verifico mossa timeout consentita*/
      probe1.send(gameServer, ClientMadeMove(TimeOut()))
      probe1.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS),ClientMoveAck(TimeoutAck()))
      probe1.expectMsgType[EndTurnUpdate](new FiniteDuration(10, TimeUnit.SECONDS))
      probe2.expectMsgType[EndTurnUpdate](new FiniteDuration(10, TimeUnit.SECONDS))
      probe1.send(gameServer, EndTurnUpdateAck())
      probe2.send(gameServer, EndTurnUpdateAck())
      probe1.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS),PlayerTurnBegins(probe2.ref))
      probe2.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS),PlayerTurnBegins(probe2.ref))
      probe1.send(gameServer, PlayerTurnBeginAck())
      probe2.send(gameServer, PlayerTurnBeginAck())
    }
  }

  "GameServer" should {

    "refuse an empty Move" in {
      val move = List[BoardTileImpl]()
      probe2.send(gameServer, ClientMadeMove(WordMove(move)))
      probe2.expectMsgType[ClientMoveAck](new FiniteDuration(10, TimeUnit.SECONDS))

      /*l'altro giocatore non deve ricevere messaggi, poichè la giocata non è valida*/
      probe1.expectNoMessage(new FiniteDuration(10, TimeUnit.SECONDS))
    }
  }

  "GameServer" should {

    "accept a correct Move" in {

      /*Se il giocatore possiede lo scarabeo passo*/
      if(!handPlayer.map(tile => tile.letter).contains("[a-zA-Z]")) {
        val dictionaryPath: String = "/dictionary/dictionary.txt"
        val dictionary: DictionaryImpl = new DictionaryImpl(dictionaryPath)

        val letters = handPlayer.map(tile => tile.letter.toLowerCase).mkString
        val moves : List[String] = Iterator.range(1, letters.length + 1)
          .flatMap(letters.combinations)
          .flatMap(_.permutations)
          .filter(word => dictionary.checkWords(List(word))).toList

        //controllo che esista almeno una parola, se non ne trovo nemmeno il giocatore è costretto a passare
        if(moves.isEmpty){
          probe2.send(gameServer, ClientMadeMove(Pass()))
          probe2.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS),ClientMoveAck(PassAck()))
        } else {
          val move : List[BoardTileImpl] = moves.head.zipWithIndex.map(c => BoardTileImpl(PositionImpl(9+c._2,9),CardImpl(c._1.toString.toUpperCase))).toList
          probe2.send(gameServer, ClientMadeMove(WordMove(move)))
          probe2.expectMsgType[ClientMoveAck](new FiniteDuration(10, TimeUnit.SECONDS))
        }
      } else {
        probe2.send(gameServer, ClientMadeMove(Pass()))
        probe2.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS),ClientMoveAck(PassAck()))
      }
      probe1.expectMsgType[EndTurnUpdate](new FiniteDuration(10, TimeUnit.SECONDS))
      probe2.expectMsgType[EndTurnUpdate](new FiniteDuration(10, TimeUnit.SECONDS))
      probe1.send(gameServer, EndTurnUpdateAck())
      probe2.send(gameServer, EndTurnUpdateAck())
      probe1.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS),PlayerTurnBegins(probe1.ref))
      probe2.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS),PlayerTurnBegins(probe1.ref))
      probe1.send(gameServer, PlayerTurnBeginAck())
      probe2.send(gameServer, PlayerTurnBeginAck())
    }
  }

}