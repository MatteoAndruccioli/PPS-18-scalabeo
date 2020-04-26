package client.controller

import client.controller.Messages.ViewToClientMessages.UserMadeHisMove
import client.controller.MoveOutcome.{AcceptedWord, HandSwitchAccepted, HandSwitchRefused, PassReceived, RefusedWord, ServerDown, TimeoutReceived}
import client.controller.MoveOutcome.ServerDown.{GameServerDown, GreetingServerDown}
import client.view.{BoardInteraction, LetterStatus, LetterTile, View}
import model.{BoardTile, Card}
import shared.Move.WordMove

/** Questo trait espone i metodi per la gestione del rapporto ClientActor GUI,
 *    permettendo passaggio di informazioni in entrambe le direzioni
 *
 */
trait ControllerLogic {

  /** Metodo chiamato quando si avvia l'applicazione e arriva il momento di mostrare la gui.
   *
   */
  def startGui(): Unit

  /** Metodo che conferma l'avvenuto login del giocatore, questo comporta il passaggio dal login al menu principale.
   *
   */
  def onLoginResponse(): Unit

  /** Quando viene trovata una partita viene mostrata una dialog che chiede all'utente se accetta di giocare.
   *
   */
  def askUserToJoinGame(): Unit

  /** Metodo che avvia la schermata di gioco.
   *
   * @param hand la mano iniziale del giocatore
   * @param players i giocatori della partita
   */
  def onMatchStart(hand:Vector[Card], players: List[String]): Unit

  /** Metodo che imposta il turno del giocatore.
   *
   */
  def userTurnBegins(): Unit

  /** Metodo chiamato quando arriva un messaggio di fine turno.
   *
   * @param ranking la classifica attuale
   * @param board l'eventuale parola giocata da un altro giocatore
   */
  def turnEndUpdates(ranking: List[(String,Int)], board:List[BoardTile]): Unit

  /** Metodo utilizzato per aggiungere una tessera in una casella del tabellone.
   *
   * @param position indice della carta giocata nella mano del giocatore
   * @param x riga del tabellone in cui si inserisce la tessera
   * @param y colonna del tabellone in cui si inserisce la tessera
   */
  def addCardToTile(position: Int, x: Int, y: Int): Unit

  /** Metodo che fa ritornare le lettere del giocatore posizionate in questo turno sul tabellone ma non ancora
   * confermate nella sua mano.
   *
   */
  def collectLetters(): Unit

  /** Metodo che fissa sul tabellone le lettere quando la parola viene accettata.
   *
   */
  def playWord(): Unit

  /** Metodo che gestisce le risposte del server in base alla giocata del giocatore.
   *
   * @param outcome il risultato della mossa del giocatore
   */
  def moveOutcome[A >: MoveOutcome](outcome: A):Unit

  /** Metodo che aggiorna la mano del giocatore in seguito alla pescata di nuove lettere.
   *
   * @param hand le carte del giocatore in questo turno.
   */
  def updateHand(hand:Vector[Card]): Unit

  /** Metodo che fa ritornare le lettere del giocatore posizionate in questo turno sul tabellone ma non ancora
   * confermate nella sua mano.
   *
   */
  def takeLettersBackInHand(): Unit

  /** Chiamato quando l'utente può continuare il proprio turno.
   *
   */
  def userTurnContinues(): Unit

  /** Determina se il giocatore può cambiare la propria mano.
   *
   * @return se la condizione è accettata o meno
   */
  def isMulliganAvailable: Boolean

  /** Nel caso in cui non si riesca a stabilire la connessione l'applicazione viene chiusa.
   *
   */
  def onConnectionFailed(): Unit

  /** Se i server per qualche motivo hanno dei problemi, allora viene notificato all'utente e viene terminata l'applicazione.
   *
   * @param server il tipo di errore del server
   */
  def serversDown(server: ServerDown):Unit

  /** Metodo che mostra all'utente una dialog quando finisce la partita.
   *
   */
  def matchEnded(player: String, playerWon:Boolean): Unit

  /** Nel caso uno dei giocatori presenti nella partita abbandona, viene notificato agli altri giocatori che vengono
   * riportati al menu principale.
   *
   */
  def playerLeft(): Unit

  /** Chiamato quando l'applicazione si deve chiudere.
   *
   */
  def terminate(): Unit

  /** Mostra un messaggio di un giocatore nella chat.
   *
   * @param sender nickname del giocatore che ha inviato il messaggio
   * @param message messaggio inviato dal giocatore
   */
  def showInChat(sender: String, message: String): Unit

  def condPrintln(verbose:Boolean)(x: Any): Unit = if (verbose) println(x)
}

object ControllerLogic {
  /** Implementazione vera e propria di ControllerLogic
   *
   *  Si occupa della gestione del rapporto ClientActor GUI,
   *    permettendo passaggio di informazioni in entrambe le direzioni
   * */
  case class CleverLogic() extends ControllerLogic {
    def startGui(): Unit = {
      new Thread(() => {
        View.main(Array[String]())
      }).start()
    }

    def onLoginResponse(): Unit = {
      View.onLoginResponse()
    }

    def askUserToJoinGame(): Unit = {
      View.askUserToJoinGame()
    }

    def onMatchStart(hand:Vector[Card], players: List[String]): Unit = {
      View.onMatchStart(hand.map(c => (c.letter, c.score)), players)
      GameManager.newGame(hand)
    }

    def userTurnBegins(): Unit = {
      View.userTurnBegins()
    }

    def turnEndUpdates(ranking: List[(String,Int)], board:List[BoardTile]): Unit = {
      View.updateLeaderboard(ranking)
      GameManager.addPlayedWordAndConfirm(board)
      View.turnEndUpdates(board.map(b => (LetterTile(60, b.card.letter, b.card.score.toString, 0, LetterStatus.insertedConfirmed), b.position.coord._1+1, b.position.coord._2+1)))
    }

    def addCardToTile(position: Int, x: Int, y: Int): Unit = {
      GameManager.addCardToTile(position, x, y)
    }

    def collectLetters(): Unit = {
      GameManager.collectLetters()
      BoardInteraction.collectLetters()
    }

    def playWord(): Unit = {
      Controller.endMyTurn()
      val playedWord = GameManager.getPlayedWord
      if(playedWord.nonEmpty) {
        playedWord.foreach(b => {
          print(b.card.letter)
        })
        Controller.sendToClient(UserMadeHisMove(WordMove(playedWord)))
      } else {
        View.showEventMessage("Devi inserire almeno una lettera per inviare la tua mossa")
        Controller.userTurnContinues()
      }
    }

    def moveOutcome[A >: MoveOutcome](outcome: A):Unit = outcome match {
      case _: RefusedWord => takeLettersBackInHand(); userTurnContinues()
      case _: HandSwitchRefused => userTurnContinues()
      case _: AcceptedWord => updateHand(outcome.asInstanceOf[AcceptedWord].hand); View.confirmPlay(); GameManager.confirmPlay()
      case _: HandSwitchAccepted => updateHand(outcome.asInstanceOf[HandSwitchAccepted].hand); Controller.endMyTurn()
      case _: PassReceived => Controller.endMyTurn()
      case _: TimeoutReceived => Controller.endMyTurn()
    }

    def updateHand(hand:Vector[Card]): Unit = {
      View.updateHand(hand.map(c => (c.letter, c.score)))
      GameManager.changeHand(hand)
    }

    def takeLettersBackInHand(): Unit = {
      View.getLettersBackFromBoard()
      GameManager.collectLetters()
    }

    def userTurnContinues(): Unit = {
      Controller.setMyTurn()
      View.userTurnContinues()
    }

    def showInChat(sender: String, message: String): Unit = {
      View.showInChat(sender, message)
    }

    def isMulliganAvailable: Boolean = {
      GameManager.isMulliganAvailable()
    }

    def onConnectionFailed(): Unit = {
      View.terminate()
    }

    def serversDown(server: ServerDown):Unit = {
      server match {
        case _: GreetingServerDown => View.greetingDisconnected()
        case _: GameServerDown => View.gameServerDisconnected()
      }
    }

    def matchEnded(player: String, playerWon:Boolean): Unit =  {
      Controller.endMyTurn()
      BoardInteraction.reset()
      View.matchEnded(player, playerWon)
    }

    def playerLeft(): Unit = {
      View.playerLeft()
    }

    def terminate(): Unit = {
      View.terminate()
    }
  }

}
