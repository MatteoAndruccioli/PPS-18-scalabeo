package server

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import server.GreetingToGameServer.InitGame
import shared.ClientMoveAckType.PassAck
import shared.ClientToGameServerMessages.{ClientMadeMove, EndTurnUpdateAck, MatchTopicListenAck, PlayerTurnBeginAck}
import shared.GameServerToClientMessages.{ClientMoveAck, EndTurnUpdate, MatchTopicListenQuery, PlayerTurnBegins}
import shared.Move.Pass

import scala.concurrent.duration.FiniteDuration

class GameServerPassTurnTest
  extends TestKit(ActorSystem("test-system"))
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

  "GameServer" must {

    "initialize himself" in {
      gameServer ! InitGame()
      topic = probe1.expectMsgType[MatchTopicListenQuery](new FiniteDuration(10, TimeUnit.SECONDS)).gameServerTopic
      probe2.expectMsgType[MatchTopicListenQuery](new FiniteDuration(10, TimeUnit.SECONDS))
      probe1.send(gameServer, MatchTopicListenAck())
      probe2.send(gameServer, MatchTopicListenAck())
      /*verifico che non riceva alcun messaggio diretto postumo*/
      probe1.expectNoMessage(new FiniteDuration(10, TimeUnit.SECONDS))
    }
  }

  "GameServer" should {

    "decide player turn" in {
      /*iscrivo ogni client al mediator*/
      val mediator = DistributedPubSub(system).mediator
      println(topic)
      mediator ! Subscribe(topic, probe1.ref)
      mediator ! Subscribe(topic, probe2.ref)
      /*verifico l'ack della sottoscrizione*/
      expectMsgType[SubscribeAck]
      expectMsgType[SubscribeAck]

      /*verifico di ricevere il messaggio del turno iniziale*/
      probe1.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS),PlayerTurnBegins(probe1.ref))
      probe2.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS),PlayerTurnBegins(probe1.ref))
      probe1.send(gameServer, PlayerTurnBeginAck())
      probe2.send(gameServer, PlayerTurnBeginAck())

      /*verifico che non succeda nulla nel server con lo scorrere del tempo poichè serve una giocata di un giocatore*/
      probe1.expectNoMessage(new FiniteDuration(120, TimeUnit.SECONDS))
      probe2.expectNoMessage(new FiniteDuration(120, TimeUnit.SECONDS))
    }
  }

  /*verifico che il server non accetti la mossa di un giocatore se non è il suo turno;
      verifico che il server accetti la mossa di un giocatore nel suo turno*/
  "GameServer" should {

    "accept pass from player in his turn" in {
      /*verifico mossa non consentita*/
      probe2.send(gameServer, ClientMadeMove(Pass()))
      probe2.expectNoMessage(new FiniteDuration(10, TimeUnit.SECONDS))
      expectNoMessage(new FiniteDuration(10, TimeUnit.SECONDS))
      /*verifico mossa consentita*/
      probe1.send(gameServer, ClientMadeMove(Pass()))
      probe1.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS),ClientMoveAck(PassAck()))
    }
  }

  /*verifico il proseguimento verso il turno successivo*/
  "GameServer" should {

    "start a new turn" in {
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

}
