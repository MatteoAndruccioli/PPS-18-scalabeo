package server

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import server.GameServerToGreeting.EndGameToGreeting
import server.GreetingToGameServer.InitGame
import shared.ChatMessages.{SendChatMessageToGameServer, SendOnChat}
import shared.ClientToGameServerMessages.{DisconnectionToGameServerNotification, MatchTopicListenAck, PlayerTurnBeginAck, SomeoneDisconnectedAck}
import shared.GameServerToClientMessages.{DisconnectionToGameServerNotificationAck, MatchTopicListenQuery, PlayerTurnBegins, SomeoneDisconnected}

import scala.concurrent.duration.FiniteDuration

class GameServerDisconnectionTest
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
  val probe3 = TestProbe()
  val probe4 = TestProbe()
  val playerList = List(probe1.ref, probe2.ref, probe3.ref, probe4.ref)
  val mapUsername = Map(probe1.ref -> "Mike", probe2.ref -> "Lisa", probe3.ref -> "Frank", probe4.ref -> "Alan")
  val gameServer = system.actorOf(Props(new GameServer(playerList, mapUsername)), "g")
  var topic: String = _

  /*verifico l'inizializzazione corretta*/
  "GameServer" must {

    "forward a chat messagge " in {
      gameServer ! InitGame()
      probe1.expectMsgType[MatchTopicListenQuery](new FiniteDuration(10, TimeUnit.SECONDS))
      topic = probe2.expectMsgType[MatchTopicListenQuery](new FiniteDuration(10, TimeUnit.SECONDS)).gameServerTopic
      probe3.expectMsgType[MatchTopicListenQuery](new FiniteDuration(10, TimeUnit.SECONDS))
      probe4.expectMsgType[MatchTopicListenQuery](new FiniteDuration(10, TimeUnit.SECONDS))
      probe1.send(gameServer, MatchTopicListenAck())
      probe2.send(gameServer, MatchTopicListenAck())
      probe3.send(gameServer, MatchTopicListenAck())
      probe4.send(gameServer, MatchTopicListenAck())

      /*iscrivo ogni client al mediator*/
      val mediator = DistributedPubSub(system).mediator
      mediator ! Subscribe(topic, probe1.ref)
      mediator ! Subscribe(topic, probe2.ref)
      mediator ! Subscribe(topic, probe3.ref)
      mediator ! Subscribe(topic, probe4.ref)
      /*verifico l'ack della sottoscrizione*/
      expectMsgType[SubscribeAck]
      expectMsgType[SubscribeAck]
      expectMsgType[SubscribeAck]
      expectMsgType[SubscribeAck]

      /*verifico di ricevere il messaggio del turno iniziale*/
      probe1.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS), PlayerTurnBegins(probe1.ref))
      probe2.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS), PlayerTurnBegins(probe1.ref))
      probe3.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS), PlayerTurnBegins(probe1.ref))
      probe4.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS), PlayerTurnBegins(probe1.ref))
      probe1.send(gameServer, PlayerTurnBeginAck())
      probe2.send(gameServer, PlayerTurnBeginAck())
      probe3.send(gameServer, PlayerTurnBeginAck())
      probe4.send(gameServer, PlayerTurnBeginAck())

      /*verifico il corretto funzionamento della chat*/
      probe1.send(gameServer, SendChatMessageToGameServer("Mike", "Good luck!"))
      probe2.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS), SendOnChat("Mike", probe1.ref, "Good luck!"))
      probe3.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS), SendOnChat("Mike", probe1.ref, "Good luck!"))
      probe4.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS), SendOnChat("Mike", probe1.ref, "Good luck!"))
      //verifico che chi manda il messaggio non lo riceva
      probe1.expectNoMessage(new FiniteDuration(10, TimeUnit.SECONDS))
    }
  }

  "GameServer" should {

    "shutdown correctly and notify it to greetingServer" in {
      /*fase di disconnessione*/
      //un client si disconnette
      probe1.send(gameServer, DisconnectionToGameServerNotification())
      probe1.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS), DisconnectionToGameServerNotificationAck())
      probe2.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS), SomeoneDisconnected())
      probe3.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS), SomeoneDisconnected())
      probe4.expectMsg(new FiniteDuration(10, TimeUnit.SECONDS), SomeoneDisconnected())
      //gli altri client rispondono, uno disconnettendosi
      probe2.send(gameServer, SomeoneDisconnectedAck())
      probe3.send(gameServer, DisconnectionToGameServerNotification())
      probe4.send(gameServer, SomeoneDisconnectedAck())
      expectMsg(new FiniteDuration(10, TimeUnit.SECONDS), EndGameToGreeting())
    }
  }
}