package client.view

import scalafx.Includes._
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.layout.GridPane
import scalafx.scene.{Group, Scene}

/** Gameview è la schermata intera del gioco. In questa schermata sono presenti tutti gli elementi che si vedono quando
 * inizia la partita.
 *
 * @param cards le tessere iniziali del giocatore
 * @param users i nomi dei giocatori nella partita
 */
class GameView(cards: Vector[(String, Int)], users: List[String]) extends PrimaryStage {
  private val WIDTH = 1280
  private val HEIGHT = 720
  private val TITLE = "Scalabeo"
  private val utilityPanel: UtilityPanel = new UtilityPanel
  private val boardAndPlayerPanel: BoardAndPlayerPanel = new BoardAndPlayerPanel(cards)
  private val legendPanel = new LegendPanel(users)
  title = TITLE
  scene = new Scene(WIDTH, HEIGHT) {
    root = new Group() {
      children = new GridPane() {
        add(legendPanel, 1, 0)
        add(utilityPanel, 2, 0)
        add(boardAndPlayerPanel, 0, 0)
      }
    }
  }

  resizable = false
  sizeToScene()
  show()

  /** Mostra un messaggio di un giocatore nella chat.
   *
   * @param sender nickname del giocatore che ha inviato il messaggio
   * @param message messaggio inviato dal giocatore
   */
  def showInChat(sender: String, message: String): Unit = {
    utilityPanel.showInChat(sender, message)
  }

  /** Stampa un messaggio in chat evidenziato di rosso per indicare che è successo qualcosa nella partita.
   *
   * @param message messaggio da inviare nella chat
   */
  def showEventMessage(message: String): Unit = {
    utilityPanel.showEventMessage(message)
  }

  /** Metodo chiamato per aggiornare lo stato grafico della board, ad esempio quando viene giocata una parola da un
   * altro giocatore.
   *
   * @param word lista delle lettere giocata e della posizione della riga e della colonna in cui sono state giocate
   */
  def updateBoard(word: List[(LetterTile, Int, Int)]): Unit = {
    boardAndPlayerPanel.updateBoard(word)
  }

  /** Metodo che verifica se il bottono per il mulligan è da disabilitare.
   *
   * @param condition condizione che specifica se il pulsante è da disabilitare
   */
  def disableMulliganButton(condition: Boolean): Unit = {
    utilityPanel.disableMulliganButton(condition)
  }

  /** Metodo che setta la GameView per l'inizio del turno del giocatore.
   *
   */
  def startTurn(): Unit = {
    utilityPanel.startTurn()
  }

  /** Metodo che fa ripartire il timer dall'inizio.
   *
   */
  def restartTimer(): Unit = {
    utilityPanel.restartTimer()
  }

  /** Metodo che ferma il timer.
   *
   */
  def pauseTimer(): Unit = {
    utilityPanel.pauseTimer()
  }

  /** Metodo che fa ripartire il timer dal punto in cui si era fermato.
   *
   */
  def resumeTimer(): Unit = {
    utilityPanel.resumeTimer()
  }

  /** Metodo chiamato per aggiornare la classifica dei giocatori.
   *
   * @param ranking la classifica della partita
   */
  def updateLeaderboard(ranking: List[(String, Int)]): Unit = {
    legendPanel.updateLeaderboard(ranking)
  }

  onCloseRequest = handle {
    View.terminate()
  }
}

