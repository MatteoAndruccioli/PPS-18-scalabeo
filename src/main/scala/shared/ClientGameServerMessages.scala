package shared

import akka.actor.ActorRef
import model.{BoardTile, Card}
import scala.collection.mutable.ArrayBuffer

//tipo dei messaggi inviati da Client a GameServer
sealed trait ClientToGameServerMessages
object ClientToGameServerMessages {
  //client ha ricevuto messaggio di inizio partita da GameServer
  case class MatchTopicListenAck() extends ClientToGameServerMessages
  //client ha ricevuto messaggio di inizio turno
  case class PlayerTurnBeginAck() extends ClientToGameServerMessages
  //messaggio che indica la mossa compiuta dall'utente
  case class ClientMadeMove(move:Move) extends ClientToGameServerMessages
  //ricevuto messaggio di fineturno
  case class EndTurnUpdateAck() extends ClientToGameServerMessages
  //ack per ricezione del messaggio di fine partita
  case class GameEndedAck() extends ClientToGameServerMessages
  //messaggio di disconnessione inviato al server in seguito a chiusura forzata UI
  case class DisconnectionToGameServerNotification() extends ClientToGameServerMessages
  //ricevuto messaggio disconnessione di un avversario
  case class SomeoneDisconnectedAck() extends ClientToGameServerMessages
}

//possibili tipi di mosse compiute dall'utente
sealed trait Move
object Move {
  //richiesta mulligan
  case class Switch() extends Move
  //posizionamento tessere
  case class WordMove(word: List[BoardTile]) extends Move
  //passa il turno
  case class Pass() extends Move
  //il tempo è scaduto prima che il cliente scegliesse una mossa corretta
  case class TimeOut() extends Move
}

//tipo dei messaggi inviati da GameServer a Client
sealed trait GameServerToClientMessages
object GameServerToClientMessages {
  //messaggio per set-up comunicazione Client-GameServer
  case class MatchTopicListenQuery(gameServerTopic:String, gameChatTopic:String, playerHand: ArrayBuffer[Card], playersList: List[String]) extends GameServerToClientMessages
  //notifica di inizio turno, playerInTurn identifica il giocatore in turno
  case class PlayerTurnBegins(playerInTurn:ActorRef) extends GameServerToClientMessages
  //ricevuta mossa dell'utente, contiene indicazioni su mossa accettata o meno
  case class ClientMoveAck(moveAckType:ClientMoveAckType) extends GameServerToClientMessages
  //notifica fineturno, contiene info per aggiornare UI
  case class EndTurnUpdate(playersRanking:List[(String,Int)], board:List[BoardTile]) extends GameServerToClientMessages
  //notifica di fine partita, le informazioni riportate individuano il vincitore
  case class GameEnded(name: String, actorRef: ActorRef) extends GameServerToClientMessages
  //ricevuta notifica di disconnessione client
  case class DisconnectionToGameServerNotificationAck() extends  GameServerToClientMessages
  //notifica di disconnessione di un avversario
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


//tipo che caratterizza messaggi inviati sulla chat
sealed trait ChatMessages
object ChatMessages{
  //messaggio che l'utente invia in chat
  case class SendChatMessageToGameServer(senderUsername: String, message: String) extends ChatMessages
  //messaggio ricevuto in chat dall'utente
  case class SendOnChat(senderUsername: String, senderActor:ActorRef, message: String) extends ChatMessages
}