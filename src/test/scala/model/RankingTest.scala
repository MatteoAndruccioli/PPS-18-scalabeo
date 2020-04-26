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
  "The players' names " should "be added to the ranking list at the start of the game" in {
    val rankingWithDefaultActor = new RankingWithDefaultActors()
    assert(rankingWithDefaultActor.ranking.ranking.nonEmpty)
    rankingWithDefaultActor.system.terminate()
  }
  "The player's points" should "be updated at the end of his turn" in{
    val points = 100
    val rankingWithDefaultActor = new RankingWithDefaultActors()
    rankingWithDefaultActor.ranking.updatePoints(rankingWithDefaultActor.actorRef2, setPoint = points)
    assert(rankingWithDefaultActor.ranking.ranking(rankingWithDefaultActor.actorRef2)==points)
    rankingWithDefaultActor.system.terminate()
  }
  "The player's points" should "be subtracted at the end of his turn" in {
    val hand = LettersHandImpl(firstHand = Vector())
    val handValue = 5
    val actorPoints = 100
    val rankingWithDefaultActor = new RankingWithDefaultActors()
    rankingWithDefaultActor.ranking.updatePoints(rankingWithDefaultActor.actorRef2, setPoint = actorPoints)
    rankingWithDefaultActor.ranking.removePoints(rankingWithDefaultActor.actorRef2, handValue)
    assert(rankingWithDefaultActor.ranking.ranking(rankingWithDefaultActor.actorRef2) == actorPoints-handValue)
    rankingWithDefaultActor.system.terminate()
  }
  "The players " should "be ordered by points" in {
    val rankingWithDefaultActor = new RankingWithDefaultActors()
    rankingWithDefaultActor.ranking.updatePoints(rankingWithDefaultActor.actorRef2, setPoint = 100)
    assert (rankingWithDefaultActor.ranking.getRankingByScore.head._1.equals(rankingWithDefaultActor.actorRef2))
    rankingWithDefaultActor.system.terminate()
  }
}
