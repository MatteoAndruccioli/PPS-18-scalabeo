package server

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import shared.ClientToGreetingMessages.{ConnectionToGreetingQuery, DisconnectionToGreetingNotification, PlayerReadyAnswer}
import shared.GameServerToClientMessages.MatchTopicListenQuery
import shared.GreetingToClientMessages.{ConnectionAnswer, DisconnectionAck, ReadyToJoinAck, ReadyToJoinQuery}
import shared.Topic.GREETING_SERVER_RECEIVES_TOPIC

import scala.concurrent.duration.FiniteDuration

class GreetingServerTest
    extends TestKit(ActorSystem("test-system"))
      with ImplicitSender
      with AnyWordSpecLike
      with Matchers
      with BeforeAndAfterAll {

    override def afterAll: Unit = {
      TestKit.shutdownActorSystem(system)
    }

    /*test iniziale nel quale si vuole verificare che, qualora il server sia attivo, questo risponda positivamente
      ad una richiesta di connessione*/
    "GreetingServer" must {

      "send positive replay when a client try to connect" in {
        val greetingServer = system.actorOf(Props(new GreetingServer()), "greetingS")
        greetingServer ! ConnectionToGreetingQuery("Mike")
        expectMsg(ConnectionAnswer(true))
      }
    }

    /*test banale nel quale si vuole verificare che, qualora il client si disconnettesse, il server abbia un
      comportamento corretto*/
    "GreetingServer" must {

      "replay with an Ack" in {

        val greetingServer = system.actorOf(Props(new GreetingServer()), "greetingSe")
        greetingServer ! DisconnectionToGreetingNotification()
        expectMsg(DisconnectionAck())
      }
    }

    /*test nel quale si vuole verificare che, al verificarsi di almeno 4 connessioni, il server proponga di unirsi ad
      una partita*/
    "GreetingServer" must {

      "ask if clients are ready to join a game when they are 4" in {
        val probe = TestProbe()
        val mediator = DistributedPubSub(system).mediator
        mediator ! Subscribe(GREETING_SERVER_RECEIVES_TOPIC, probe.ref)

        val greetingServer = system.actorOf(Props(new GreetingServer()), "greetingSer")
        expectMsgType[SubscribeAck]
        greetingServer ! ConnectionToGreetingQuery("Mike")
        expectMsg(ConnectionAnswer(true))
        greetingServer ! ConnectionToGreetingQuery("Carl")
        expectMsg(ConnectionAnswer(true))
        greetingServer ! ConnectionToGreetingQuery("Lenny")
        expectMsg(ConnectionAnswer(true))
        greetingServer ! ConnectionToGreetingQuery("Clara")
        expectMsg(ConnectionAnswer(true))
        probe.expectMsg(ReadyToJoinQuery())
      }
    }

  /*test nel quale si vuole verificare che con 4 risposte positive da parte degli utenti, una partita viene istanziata
        correttament. Un gameServer la gestirà. */
  "GreetingServer" must {

    "not create the GameServer" in {

      val probe = TestProbe()
      val mediator = DistributedPubSub(system).mediator
      mediator ! Subscribe(GREETING_SERVER_RECEIVES_TOPIC, probe.ref)

      val greetingServer = system.actorOf(Props(new GreetingServer()), "greetingServ")
      expectMsgType[SubscribeAck]
      greetingServer ! ConnectionToGreetingQuery("Mike")
      expectMsg(ConnectionAnswer(true))
      greetingServer ! ConnectionToGreetingQuery("Carl")
      expectMsg(ConnectionAnswer(true))
      greetingServer ! ConnectionToGreetingQuery("Lenny")
      expectMsg(ConnectionAnswer(true))
      greetingServer ! ConnectionToGreetingQuery("Clara")
      expectMsg(ConnectionAnswer(true))
      probe.expectMsg(ReadyToJoinQuery())
      greetingServer ! PlayerReadyAnswer(true)
      greetingServer ! PlayerReadyAnswer(true)
      greetingServer ! PlayerReadyAnswer(true)
      greetingServer ! PlayerReadyAnswer(false)
      expectMsgAllOf(ReadyToJoinAck(),ReadyToJoinAck(),ReadyToJoinAck(),ReadyToJoinAck())
      expectNoMessage(new FiniteDuration(10, TimeUnit.SECONDS))
    }
  }

    /*test nel quale si vuole verificare che con 4 risposte positive da parte degli utenti, una partita viene istanziata
        correttamente. Un gameServer la gestirà. */
    "GreetingServer" should {

      "choose the turn" in {

        val probe = TestProbe()
        val mediator = DistributedPubSub(system).mediator
        mediator ! Subscribe(GREETING_SERVER_RECEIVES_TOPIC, probe.ref)

        val greetingServer = system.actorOf(Props(new GreetingServer()), "greetingServe")
        expectMsgType[SubscribeAck]
        greetingServer ! ConnectionToGreetingQuery("Mike")
        expectMsg(ConnectionAnswer(true))
        greetingServer ! ConnectionToGreetingQuery("Carl")
        expectMsg(ConnectionAnswer(true))
        greetingServer ! ConnectionToGreetingQuery("Lenny")
        expectMsg(ConnectionAnswer(true))
        greetingServer ! ConnectionToGreetingQuery("Clara")
        expectMsg(ConnectionAnswer(true))
        probe.expectMsg(ReadyToJoinQuery())
        greetingServer ! PlayerReadyAnswer(true)
        greetingServer ! PlayerReadyAnswer(true)
        greetingServer ! PlayerReadyAnswer(true)
        greetingServer ! PlayerReadyAnswer(true)
        expectMsgAllOf(ReadyToJoinAck(),ReadyToJoinAck(),ReadyToJoinAck(),ReadyToJoinAck())
        expectMsgType[MatchTopicListenQuery](new FiniteDuration(10, TimeUnit.SECONDS))
        expectMsgType[MatchTopicListenQuery](new FiniteDuration(10, TimeUnit.SECONDS))
        expectMsgType[MatchTopicListenQuery](new FiniteDuration(10, TimeUnit.SECONDS))
        expectMsgType[MatchTopicListenQuery](new FiniteDuration(10, TimeUnit.SECONDS))
      }
    }
  }
