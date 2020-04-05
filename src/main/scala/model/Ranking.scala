package model


import akka.actor.ActorRef
import scala.collection.immutable.ListMap

// classifica dei giocatori
sealed trait Ranking {
  def ranking: Map[ActorRef, Int]
  def addInitialPlayers(matchPlayer: List[ActorRef]): Map[ActorRef, Int]
  def updatePoints (actorRef: ActorRef, setPoint: Int)
  def removePoints(actorRef: ActorRef, handPoint: Int)
  def getRankingByScore: ListMap[ActorRef, Int]
}

// implementazione della classifica dei giocatori
class RankingImpl(players: List[ActorRef]) extends Ranking {
  private var _ranking: Map[ActorRef, Int] = addInitialPlayers(players)
  override def ranking: Map[ActorRef, Int] = _ranking
  // metodo per aggiungere i giocatori iniziali
  override def addInitialPlayers(matchPlayers: List[ActorRef]): Map[ActorRef, Int]= {
    var map: Map[ActorRef, Int] = Map()
    matchPlayers.foreach(player => map = map.+((player, 0)))
    map
  }
  // metodo per aggiornamento dei punti di un giocatore
  override def updatePoints(player: ActorRef, setPoints: Int): Unit = _ranking =_ranking.updated(player, _ranking.getOrElse(player,0) + setPoints)
  // metodo per rimuovere i punti per un giocatore
  override def removePoints(player: ActorRef, handPoint: Int): Unit = _ranking =_ranking.updated(player, _ranking.getOrElse(player,0) - handPoint)
  // metodo per ottenere l'elenco dei giocatori ordinati per punteggio
  override def getRankingByScore:ListMap[ActorRef, Int] = ListMap(_ranking.toSeq.sortWith(_._2 > _._2):_*)
}
