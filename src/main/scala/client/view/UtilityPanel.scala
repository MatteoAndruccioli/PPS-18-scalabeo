package client.view

import client.controller.Controller
import client.controller.Messages.ViewToClientMessages.UserMadeHisMove
import scalafx.scene.control.Button
import scalafx.scene.layout.{GridPane, HBox, VBox}
import scalafx.Includes.{handle, _}
import shared.Move.{Pass, Switch}

/** Classe che include il timer, i pulsanti di gioco e la chat.
 *
 */
class UtilityPanel extends GridPane {
  private val WIDTH = 310
  private val HEIGHT = 720
  private val SEND_BUTTON_TEXT = "Gioca"
  private val MULLIGAN_BUTTON_TEXT = "Mulligan"
  private val PASS_BUTTON_TEXT = "Passo"
  private val RETIRE_LETTERS_BUTTON_TEXT = "Ritira Lettere"
  stylesheets = List("/style/UtilityPanel.css")
  styleClass += "body"
  prefWidth = WIDTH
  prefHeight = HEIGHT
  val timerPanel: TimerPanel = new TimerPanel
  val chatPanel: ChatPanel = new ChatPanel
  val mulliganButton: Button = new Button(MULLIGAN_BUTTON_TEXT) {
    onAction = handle {
      if(Controller.isMyTurn) {
        timerPanel.pauseTimer()
        Controller.endMyTurn()
        View.sendToClient(UserMadeHisMove(Switch()))
        mulliganButton.disable = true
      } else {
        chatPanel.showEventMessage("Non è il tuo turno!")
      }
    }
  }

  add(new VBox() {
    styleClass += "timer-container"
    children = timerPanel
  }, 0, 0)

  add(new VBox(5){
    children = List(
      new HBox(5) {
        styleClass += "button-container"
        children = List(new Button(SEND_BUTTON_TEXT) {
          onAction = handle {
            if(Controller.isMyTurn) {
              timerPanel.pauseTimer()
              Controller.playWord()
            } else {
              chatPanel.showEventMessage("Non è il tuo turno!")
            }
          }
        },
          new Button(PASS_BUTTON_TEXT) {
            onAction = handle {
              if(Controller.isMyTurn) {
                timerPanel.pauseTimer()
                Controller.collectLetters()
                View.sendToClient(UserMadeHisMove(Pass()))
              } else {
                chatPanel.showEventMessage("Non è il tuo turno!")
              }
            }
          }
        )
      },
      new HBox(5) {
        styleClass += "button-container"
        children = List(
          mulliganButton,
          new Button(RETIRE_LETTERS_BUTTON_TEXT) {
            onAction = handle {
              Controller.collectLetters()
              chatPanel.showEventMessage("Hai ritirato le lettere che avevi messo sul tabellone")
            }
          }
        )
      }
    )
  }, 0, 1)

  add(new VBox() {
    children = List(
      new VBox() {
        styleClass += "big-container"
        children = List(
          new HBox(5) {
            styleClass += "line-container"
            children = List(
              LetterTile(36, "s", "", 0, LetterStatus.placeHolder),
              LetterTile(36, "c", "", 0, LetterStatus.placeHolder),
              LetterTile(36, "a", "", 0, LetterStatus.placeHolder),
              LetterTile(36, "l", "", 0, LetterStatus.placeHolder),
              LetterTile(36, "a", "", 0, LetterStatus.placeHolder),
              LetterTile(36, "b", "", 0, LetterStatus.placeHolder),
              LetterTile(36, "e", "", 0, LetterStatus.placeHolder),
              LetterTile(36, "o", "", 0, LetterStatus.placeHolder)
            )
          },
          new HBox(5) {
            styleClass += "line-container"
            children = List(
              LetterTile(36, "c", "", 0, LetterStatus.placeHolder),
              LetterTile(36, "h", "", 0, LetterStatus.placeHolder),
              LetterTile(36, "a", "", 0, LetterStatus.placeHolder),
              LetterTile(36, "t", "", 0, LetterStatus.placeHolder),
            )
          }
        )
      },
      new VBox() {
        styleClass += "chat-container"
        children = chatPanel
      }

    )
  }, 0, 2)

  /** Mostra un messaggio di un giocatore nella chat.
   *
   * @param sender nickname del giocatore che ha inviato il messaggio
   * @param message messaggio inviato dal giocatore
   */
  def showInChat(sender: String, message: String): Unit = {
    chatPanel.showInChat(sender, message)
  }

  /** Stampa un messaggio in chat evidenziato di rosso per indicare che è successo qualcosa nella partita.
   *
   * @param message messaggio da inviare nella chat
   */
  def showEventMessage(message: String): Unit = {
    chatPanel.showEventMessage(message)
  }

  /** Metodo che verifica se il bottono per il mulligan è da disabilitare.
   *
   * @param condition condizione che specifica se il pulsante è da disabilitare
   */
  def disableMulliganButton(condition: Boolean): Unit = {
    mulliganButton.disable = condition
  }

  /** Metodo chiamato quando inizia il turno del giocatore.
   *
   */
  def startTurn(): Unit = {
    timerPanel.startTurn()
  }

  /** Restarta il timer dall'inizio.
   *
   */
  def restartTimer(): Unit = {
    timerPanel.restartTimer()
  }

  /** Ferma il timer in un momento preciso.
   *
   */
  def pauseTimer(): Unit = {
    timerPanel.pauseTimer()
  }

  /** Il timer riprende da dove era stato messo in pausa.
   *
   */
  def resumeTimer(): Unit = {
    timerPanel.resumeTimer()
  }
}


