package model

import akka.actor.ActorRef
import scala.collection.immutable.ListMap


/** Classifica dei giocatori di una partita
  * - ranking: classifica
  * - updatePoints: metodo per aggiornamento dei punti di un giocatore
  * - removePoints: metodo per rimuovere i punti per un giocatore
  * - getRankingByScore: metodo per ottenere l'elenco dei giocatori ordinati per punteggio
  */
sealed trait Ranking {
  def ranking: Map[ActorRef, Int]
  def updatePoints (actorRef: ActorRef, setPoint: Int)
  def removePoints(actorRef: ActorRef, handPoint: Int)
  def getRankingByScore: ListMap[ActorRef, Int]
}

/**Implementazione della classifica dei giocatori
  * @param players: lista dei giocatori della partita
  */
class RankingImpl(players: List[ActorRef]) extends Ranking {
  private var _ranking: Map[ActorRef, Int] = addInitialPlayers(players)
  /** metodo per accedere alla classifica
    * @return la classifica
    */
  override def ranking: Map[ActorRef, Int] = _ranking
  /** metodo per aggiungere i giocatori iniziali
    * @param matchPlayers: lista dei giocatori della partita
    * @return mappa iniziale dei giocatori
    */
  private def addInitialPlayers(matchPlayers: List[ActorRef]): Map[ActorRef, Int]= {
    var map: Map[ActorRef, Int] = Map()
    matchPlayers.foreach(player => map = map.+((player, 0)))
    map
  }
  /** metodo per aggiornamento dei punti di un giocatore
    * @param player: giocatore a cui aggiornare i punti
    * @param setPoints: punti da sommare
    */
  override def updatePoints(player: ActorRef, setPoints: Int): Unit = _ranking =_ranking.updated(player, _ranking.getOrElse(player,0) + setPoints)
  /** metodo per rimuovere i punti per un giocatore
    * @param player: giocatore a cui sottrarre i punti
    * @param handPoint: punti da sottrarre
    */
  override def removePoints(player: ActorRef, handPoint: Int): Unit = _ranking =_ranking.updated(player, _ranking.getOrElse(player,0) - handPoint)
  /** metodo per ottenere l'elenco dei giocatori ordinati per punteggio
    * @return lista dei giocatori ordinati per punteggio
    */
  override def getRankingByScore:ListMap[ActorRef, Int] = ListMap(_ranking.toSeq.sortWith(_._2 > _._2):_*)
}
