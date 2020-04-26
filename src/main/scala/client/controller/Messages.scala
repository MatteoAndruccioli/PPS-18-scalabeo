package client.controller

import shared.Move

object Messages {
  /** Trait dei messaggi inviati dalla UI a ClientActor */
  sealed trait ViewToClientMessages
  /** Contiene l'implementazione di messaggi inviati da UI a ClientActor */
  object ViewToClientMessages {
    /** messaggio contenente lo username scelto dal giocatore */
    case class UsernameChosen(username: String) extends ViewToClientMessages
    /** giocatore chiede di essere messo in coda per giocare */
    case class JoinQueue() extends ViewToClientMessages
    /** giocatore esprime disponibilità a entrare in una partita */
    case class UserReadyToJoin(ready:Boolean) extends ViewToClientMessages
    /** giocatore vuole giocare una nuova partita */
    case class PlayAgain(userWantsToPlay:Boolean) extends ViewToClientMessages
    /** il giocatore ha chiuso il gioco forzatamente */
    case class UserExited() extends  ViewToClientMessages
    /** giocatore indica la propria mossa */
    case class UserMadeHisMove(move: Move) extends ViewToClientMessages
    /** messaggio che il giocatore vuole inviare in chat */
    case class ChatMessage(message: String) extends ViewToClientMessages
  }
}
