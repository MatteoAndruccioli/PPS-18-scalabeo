package shared

//messaggi scambiati tra client e server
//sono solo dei messaggi di prova per verificare che la comunicazione funzioni
sealed trait DemoMessage
object DemoMessage {
  //messaggio inviato dal Client
  case class ClientMessage (message:String) extends DemoMessage
  //messaggio inviato dal Server
  case class ServerMessage (message:String) extends DemoMessage
}
