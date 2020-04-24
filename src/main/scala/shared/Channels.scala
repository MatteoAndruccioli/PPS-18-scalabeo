package shared

/** Contiene le stringhe con le quali vengono costruiti topic condivisi dai vari Client e i server
 */
object Channels {
  /** il greeting server comunica con i client attraverso questo canale */
  val GREETING_SERVER_RECEIVES_TOPIC : String = "GreetingServerReceiveTopic"
  /** pattern iniziale con il quale un server stabilisce il suo game-topic univoco */
  val GAME_SERVER_SEND_TOPIC : String = "GameServerSendTopic"
  /** pattern iniziale con il quale un server stabilisce il suo chat-topic univoco */
  val CHAT_TOPIC : String = "ChatTopic"
}
