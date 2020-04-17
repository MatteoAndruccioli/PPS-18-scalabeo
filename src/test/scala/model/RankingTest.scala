package model

import scala.collection.mutable.ArrayBuffer
import org.scalatest._
import server.GreetingServer
import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

class RankingWithDefaultActors {
  val system: ActorSystem = ActorSystem.create(name="ClusterSystem", ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 0)
    .withFallback(ConfigFactory.parseString("akka.cluster.roles = [serverRole]")).withFallback(ConfigFactory.load()))
  val actorRef1: ActorRef = system.actorOf(Props.create(classOf[GreetingServer]), name="Actor1")
  val actorRef2: ActorRef = system.actorOf(Props.create(classOf[GreetingServer]), name="Actor2")
  val ranking: Ranking = new RankingImpl(List(actorRef1, actorRef2))
}

class RankingTest extends FlatSpec {
  "The list of the players " should "be added to the ranking at the start of the game" in {
    val rankingWithDefaultActor = new RankingWithDefaultActors()
    assert(rankingWithDefaultActor.ranking.ranking.nonEmpty)
    rankingWithDefaultActor.system.terminate()
  }
  "The points of one player " should "be updated at the end of his turn" in{
    val points = 100
    val rankingWithDefaultActor = new RankingWithDefaultActors()
    rankingWithDefaultActor.ranking.updatePoints(rankingWithDefaultActor.actorRef2, setPoint = points)
    assert(rankingWithDefaultActor.ranking.ranking(rankingWithDefaultActor.actorRef2)==points)
    rankingWithDefaultActor.system.terminate()
  }
  "The points of an hand " should "be subtracted from the player points" in {
    val hand = LettersHandImpl(new ArrayBuffer[Card]())
    val handValue = 5
    val actorPoints = 100
    val rankingWithDefaultActor = new RankingWithDefaultActors()
    hand.hand+=(CardImpl("A"),CardImpl("B"))
    rankingWithDefaultActor.ranking.updatePoints(rankingWithDefaultActor.actorRef2, setPoint = actorPoints)
    rankingWithDefaultActor.ranking.removePoints(rankingWithDefaultActor.actorRef2, hand.calculateHandPoint)
    assert(rankingWithDefaultActor.ranking.ranking(rankingWithDefaultActor.actorRef2) == actorPoints-handValue)
    rankingWithDefaultActor.system.terminate()
  }
  "The players " should "be list by points" in {
    val rankingWithDefaultActor = new RankingWithDefaultActors()
    rankingWithDefaultActor.ranking.updatePoints(rankingWithDefaultActor.actorRef2, setPoint = 100)
    assert (rankingWithDefaultActor.ranking.getRankingByScore.head._1.equals(rankingWithDefaultActor.actorRef2))
    rankingWithDefaultActor.system.terminate()
  }
}
