package shared

object Topic {
  //il greeting server comunica con i client attraverso questo canale
  val GREETING_SERVER_RECEIVES_TOPIC : String = "GreetingServerReceiveTopic"
  val GAME_SERVER_SEND_TOPIC : String = "GameServerSendTopic"
}
