package client.view

import client.controller.Controller
import client.controller.Messages.ViewToClientMessages.UserMadeHisMove
import scalafx.scene.control.Button
import scalafx.scene.layout.{GridPane, HBox, VBox}
import scalafx.Includes.{handle, _}
import shared.Move.{Pass, Switch}

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

  add(new HBox(5) {
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
      },
      mulliganButton,
      new Button(RETIRE_LETTERS_BUTTON_TEXT) {
        onAction = handle {
          Controller.collectLetters()
          BoardInteraction.collectLetters()
          chatPanel.showEventMessage("Hai ritirato le lettere che avevi messo sul tabellone")
        }
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

  def showInChat(sender: String, message: String): Unit = {
    chatPanel.showInChat(sender, message)
  }

  def showEventMessage(message: String): Unit = {
    chatPanel.showEventMessage(message)
  }

  def disableMulliganButton(condition: Boolean): Unit = {
    mulliganButton.disable = condition
  }

  def startTurn(): Unit = {
    timerPanel.startTurn()
  }

  def restartTimer(): Unit = {
    timerPanel.restartTimer()
  }

  def pauseTimer(): Unit = {
    timerPanel.pauseTimer()
  }

  def resumeTimer(): Unit = {
    timerPanel.resumeTimer()
  }
}


