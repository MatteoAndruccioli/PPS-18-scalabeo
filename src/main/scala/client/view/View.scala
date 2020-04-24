package client.view

import client.controller.Controller
import client.controller.Messages.ViewToClientMessages
import client.controller.Messages.ViewToClientMessages.{PlayAgain, UserExited}
import scalafx.application.{JFXApp, Platform}

/** Classe che contiene i metodi per interagire con il controller.
 *
 */
object View extends JFXApp {
  private val PLAYER_EXITED_DIALOG_TEXT = "A player left the game, in 5 seconds you will be redirected to the main menu."
  private val SERVER_CRASHED_DIALOG_TEXT = "The main server has crashed, the game will exit in 5 seconds."
  private val GAME_SERVER_CRASHED_TEXT = "The game server has crashed, in 5 seconds you will be redirected to the main menu."
  var mainMenu: MainMenu = new MainMenu
  var gameBoard: GameView = _
  stage = mainMenu

  /** Metodo chiamato quando l'utente effettua con successo il login.
   *
   */
  def onLoginResponse(): Unit = {
    mainMenu.onLoginResponse()
  }

  /** Metodo chiamato per inviare un messaggio al client.
   *
   * @param message messaggio da inviare al client
   */
  def sendToClient(message: ViewToClientMessages): Unit = {
    Controller.sendToClient(message)
  }

  /** Metodo che avvia la schermata di gioco.
   *
   * @param cards la mano iniziale del giocatore
   * @param players i giocatori della partita
   */
  def onMatchStart(cards: Vector[(String, Int)], players:List[String]): Unit = {
    Platform.runLater(() => {
      if(stage != null)
        stage.close()
      gameBoard = new GameView(cards, players)
    })
  }

  /** Metodo che mostra all'utente una dialog quando viene trovata una partita.
   *
   */
  def askUserToJoinGame(): Unit = {
    mainMenu.askUserToJoinGame()
  }

  /** Metodo che permette dalla schermata di gioco di tornare al menu principale.
   *
   */
  def backToMainMenu(): Unit = {
    Platform.runLater(() =>{
      gameBoard.close()
      mainMenu = new MainMenu
      mainMenu.onLoginResponse()
      stage = mainMenu
      stage.show()
    })
  }

  /** Metodo che mostra all'utente una dialog quando un giocatore abbandona la partita.
   *
   */
  def playerLeft(): Unit = {
    Platform.runLater(() =>{
      new Dialog(PLAYER_EXITED_DIALOG_TEXT)
        .autoClose(Option(gameBoard), () => {
          mainMenu = new MainMenu
          mainMenu.onLoginResponse()
          stage = mainMenu
          stage.show()
        })
        .show()
    })
  }

  /** Metodo che mostra all'utente una dialog quando il GreetingServer si disconnette.
   *
   */
  def greetingDisconnected(): Unit = {
    Platform.runLater(() =>{
      new Dialog(SERVER_CRASHED_DIALOG_TEXT)
        .autoClose(Option(gameBoard), () => {
          Controller.exit()
        })
        .show()
    })
  }

  /** Metodo che mostra all'utente una dialog quando il GameServer si disconnette.
   *
   */
  def gameServerDisconnected(): Unit = {
    Platform.runLater(() =>{
      new Dialog(GAME_SERVER_CRASHED_TEXT)
        .autoClose(Option(gameBoard), () => {
          mainMenu = new MainMenu
          mainMenu.onLoginResponse()
          stage = mainMenu
          stage.show()
        })
        .show()
    })
  }

  /** Metodo che imposta il turno del giocatore.
   *
   */
  def userTurnBegins(): Unit = {
    Platform.runLater(() => {
      gameBoard.disableMulliganButton(!Controller.isMulliganAvailable)
      gameBoard.startTurn()
      gameBoard.restartTimer()
      showEventMessage("E' il tuo turno!")
    })
  }

  /** Metodo chiamato quando arriva un messaggio di fine turno.
   *
   * @param word l'eventuale parola giocata da un altro giocatore
   */
  def turnEndUpdates(word: List[(LetterTile, Int, Int)]): Unit = {
    gameBoard.updateBoard(word)
  }

  /** Mostra un messaggio di un giocatore nella chat.
   *
   * @param sender nickname del giocatore che ha inviato il messaggio
   * @param message messaggio inviato dal giocatore
   */
  def showInChat(sender: String, message: String): Unit = {
    Platform.runLater(() => {
      gameBoard.showInChat(sender, message)
    })
  }

  /** Stampa un messaggio in chat evidenziato di rosso per indicare che è successo qualcosa nella partita.
   *
   * @param message messaggio da inviare nella chat
   */
  def showEventMessage(message: String): Unit = {
    Platform.runLater(() => {
      gameBoard.showEventMessage(message)
    })
  }

  /** Metodo chiamato quando finisce il turno del giocatore.
   *
   */
  def endMyTurn(): Unit = {
    Controller.endMyTurn()
  }

  /** Metodo che aggiorna la mano del giocatore in seguito alla pescata di nuove lettere.
   *
   * @param cards le carte del giocatore in questo turno.
   */
  def updateHand(cards: Vector[(String, Int)]): Unit = {
    BoardInteraction.updateHand(cards)
  }

  /** Metodo che fa ritornare le lettere del giocatore posizionate in questo turno sul tabellone ma non ancora
   * confermate nella sua mano.
   *
   */
  def getLettersBackFromBoard(): Unit = {
    BoardInteraction.collectLetters()
  }

  /** Chiamato quando l'utente può continuare il proprio turno.
   *
   */
  def userTurnContinues(): Unit = {
    gameBoard.resumeTimer()
  }

  /** Metodo chiamato per aggiornare la classifica dei giocatori.
   *
   * @param ranking la classifica della partita
   */
  def updateLeaderboard(ranking: List[(String, Int)]): Unit = {
    gameBoard.updateLeaderboard(ranking)
  }

  /** Metodo chiamato quando la parola giocata dall'utente è stata accetata, di conseguenza non è più possibile spostare
   * le lettere che il giocatore ha posizionato in qeusto turno.
   *
   */
  def confirmPlay(): Unit = {
    BoardInteraction.confirmPlay()
  }

  /** Metodo che mostra all'utente una dialog quando finisce la partita.
   *
   */
  def matchEnded(player: String, playerWon: Boolean): Unit = {
    var winnerString = ""
    if(playerWon) {
      winnerString = "Hai vinto!!! Vuoi giocare di nuovo?"
    } else {
      winnerString = player + " ha vinto. Vuoi giocare di nuovo?"
    }
    Platform.runLater(() => {
        new Dialog(winnerString)
          .addYesNoButtons(
            () => {
              View.sendToClient(PlayAgain(true))
              View.backToMainMenu()
            },
            () => {
              View.sendToClient(PlayAgain(false))
              View.terminate()
            }).show()
      })
  }

  /** Metodo chiamato quando si chiude l'applicazione.
   *
   */
  def terminate(): Unit  = {
    View.sendToClient(UserExited())
    Platform.exit()
  }

}

class View {}

