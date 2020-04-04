package server

sealed trait GreetingToGameServer
object GreetingToGameServer {
  case class InitGame() extends GreetingToGameServer
  case class EndGameToGreetingAck() extends GreetingToGameServer
}

sealed trait GameServerToGameServerMessages
object GameServerToGameServer{
  case class EndGameInit() extends GameServerToGameServerMessages
}

sealed trait GameServerToGreeting
object GameServerToGreeting {
  case class EndGameToGreeting() extends GameServerToGreeting
}