package shared

/** Messaggi che vengono inviati da Client a GreetingServer */
sealed trait ClientToGreetingMessages
/** Oggetto contenente messaggi che vengono inviati da Client a GreetingServer */
object ClientToGreetingMessages {
  /** richiesta di nuova connessione al server, viene comunicato l'username del player */
  case class ConnectionToGreetingQuery(username: String) extends ClientToGreetingMessages
  /** player comunica se è ancora disponibile a giocare */
  case class PlayerReadyAnswer(playerAccepts: Boolean) extends ClientToGreetingMessages
  /** client comunica disconnessione al server */
  case class DisconnectionToGreetingNotification() extends ClientToGreetingMessages
}



/** Messaggi che vengono inviati da GreetingServer a Client */
sealed trait GreetingToClientMessages
/** Oggetto contenente messaggi che vengono inviati da Client a GreetingServer */
object GreetingToClientMessages {
  /** risposta alla richiesta di connessione */
  case class ConnectionAnswer(connected: Boolean) extends GreetingToClientMessages
  /** messaggio per verificare che il player sia ancora disponibile */
  case class ReadyToJoinQuery() extends GreetingToClientMessages
  /** conferma di ricezione messaggio riguardo disponibilità del player */
  case class ReadyToJoinAck() extends GreetingToClientMessages
  /** conferma di ricezione messaggio di disconnessione */
  case class DisconnectionAck() extends GreetingToClientMessages
}
