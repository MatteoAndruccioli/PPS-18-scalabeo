package model

import scala.collection.mutable.ArrayBuffer
import org.scalatest._
import server.GreetingServer
import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory


class RankingTest extends FlatSpec {

  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2551)
    .withFallback(ConfigFactory.parseString("akka.cluster.roles = [serverRole]")).withFallback(ConfigFactory.load())
  val system = ActorSystem.create(name="ClusterSystem", config)
  val actorRef1 : ActorRef = system.actorOf(Props.create(classOf[GreetingServer]), name="Actor1")
  val actorRef2 : ActorRef = system.actorOf(Props.create(classOf[GreetingServer]), name="Actor2")
  val playerList = List(actorRef1, actorRef2)
  val ranking = new RankingImpl(playerList)

  "The list of the players " should "be added to the ranking at the start of the game" in assert(ranking.ranking.nonEmpty)

  "The point of one player " should "be updated at the end of his turn" in{
    val points = 100
    ranking.updatePoints(actorRef2, setPoints = points)
    assert(ranking.ranking(actorRef2)==points)
  }

  "The points of an hand " should "be subtracted from the player points" in {
    val hand = LettersHandImpl(new ArrayBuffer[Card]())
    val handValue = 5
    val actorPoints = 100
    hand.hand+=(CardImpl("A"),CardImpl("B"))
    ranking.removePoints(actorRef2, hand.calculateHandPoint)
    assert(ranking.ranking(actorRef2) == actorPoints-handValue)
  }

  "the players " should "be list by points" in assert (ranking.getRankingByScore.head._1.equals(actorRef2))

  system.stop(actorRef1)
  system.stop(actorRef2)
}
