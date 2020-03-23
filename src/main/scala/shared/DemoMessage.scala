package shared

//messaggi scambiati tra client e server
//sono solo dei messaggi di prova per verificare che la comunicazione funzioni
sealed trait DemoMessage
object DemoMessage {
  //messaggio inviato dal Client sul mediator
  case class ClientMediatorMessage (message:String) extends DemoMessage
  //messaggio inviato dal Server
  case class ServerMediatorMessage (message:String) extends DemoMessage
  //ack inviato dal client alla ricezione di un messaggio ServerMediatorMessage
  case class ClientAck () extends DemoMessage
  //ack inviato dal server alla ricezione di un messaggio ClientMediatorMessage
  case class ServerAck () extends DemoMessage
}
