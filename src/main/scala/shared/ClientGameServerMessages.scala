package shared

import akka.actor.ActorRef
import model.{BoardTile, Card}

import scala.collection.mutable.ArrayBuffer

//tipo dei messaggi inviati da Client a GameServer
sealed trait ClientToGameServerMessages
object ClientToGameServerMessages {
  case class MatchTopicListenAck() extends ClientToGameServerMessages
  case class PlayerTurnBeginAck() extends ClientToGameServerMessages
  case class ClientMadeMove(move:Move) extends ClientToGameServerMessages
  case class EndTurnUpdateAck() extends ClientToGameServerMessages
  //ack per ricezione del messaggio di fine partita
  case class GameEndedAck() extends ClientToGameServerMessages
  //messaggio di disconnessione inviato al server in seguito a chiusura forzata UI
  case class DisconnectionToGameServerNotification() extends ClientToGameServerMessages
  case class SomeoneDisconnectedAck() extends ClientToGameServerMessages
}

sealed trait Move
object Move {
  case class Switch() extends Move
  case class WordMove(word: List[BoardTile]) extends Move
  case class Pass() extends Move
  case class TimeOut() extends Move
}

//tipo dei messaggi inviati da GameServe a Client
sealed trait GameServerToClientMessages
object GameServerToClientMessages {
  case class MatchTopicListenQuery(gameServerTopic:String, playerHand: ArrayBuffer[Card], playersList: List[String]) extends GameServerToClientMessages
  case class PlayerTurnBegins(playerInTurn:ActorRef) extends GameServerToClientMessages
  case class ClientMoveAck(moveAckType:ClientMoveAckType) extends GameServerToClientMessages
  case class EndTurnUpdate(playersRanking:List[(String,Int)], board:List[BoardTile]) extends GameServerToClientMessages
  case class GameEnded(name: String, actorRef: ActorRef) extends GameServerToClientMessages
  case class DisconnectionToGameServerNotificationAck() extends  GameServerToClientMessages
  case class SomeoneDisconnected() extends  GameServerToClientMessages
}

sealed trait ClientMoveAckType
object ClientMoveAckType{
  case class WordAccepted(hand:ArrayBuffer[Card]) extends ClientMoveAckType //la mossa dell'utente era la composizione di una parola e la parola viene accettata
  case class WordRefused() extends ClientMoveAckType//la mossa dell'utente era la composizione di una parola che viene rifiutata
  case class HandSwitchRequestAccepted(hand:ArrayBuffer[Card]) extends ClientMoveAckType//la mossa dell'utente era una richiesta di cambio mano che viene accettata
  case class HandSwitchRequestRefused() extends ClientMoveAckType//la mossa dell'utente era una richiesta di cambio mano che viene rifiutata
  case class PassAck() extends ClientMoveAckType //la mossa dell'utente era un passo => accettato sempre
  case class TimeoutAck() extends ClientMoveAckType //l'utente non ha prodotto nessuna mossa e il timer è scaduto => accettato sempre
}