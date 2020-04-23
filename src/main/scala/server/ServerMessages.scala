package server

/** Messaggi che vengono inviati da GreetingServer a Gameserver
 */
sealed trait GreetingToGameServer
object GreetingToGameServer {
  //notifica di inizio partita
  case class InitGame() extends GreetingToGameServer
  //notifica di ricezione della fine di una partita
  case class EndGameToGreetingAck() extends GreetingToGameServer
}

/** Messaggi che vengono inviati da Gameserver a se stesso
 */
sealed trait GameServerToGameServerMessages
object GameServerToGameServer{
  //notifica di cambio di comportamento
  case class EndGameInit() extends GameServerToGameServerMessages
}

/** Messaggi che vengono inviati da Gameserver a GreetingServer
 */
sealed trait GameServerToGreeting
object GameServerToGreeting {
  //notifica di fine partita
  case class EndGameToGreeting() extends GameServerToGreeting
}