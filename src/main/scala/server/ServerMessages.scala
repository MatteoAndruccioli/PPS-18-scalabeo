package server

sealed trait GreetingToGameServer
object GreetingToGameServer {
  case class InitGame() extends GreetingToGameServer
}