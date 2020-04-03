package client.view

import client.controller.Controller
import client.controller.Messages.ViewToClientMessages.UserMadeHisMove
import scalafx.scene.control.Button
import scalafx.scene.layout.{GridPane, HBox, VBox}
import scalafx.Includes.{handle, _}
import shared.Move.{Pass, Switch}

class UtilityPanel extends GridPane {
  stylesheets = List("/style/UtilityPanel.css")
  styleClass += "body"
  prefWidth = 310
  prefHeight = 720
  val timerPanel: TimerPanel = new TimerPanel
  timerPanel.startTurn()

  val mulliganButton: Button = new Button("Mulligan") {
    onAction = handle {
      if(Controller.isMyTurn) {
        println("Switchato")
        timerPanel.pauseTimer()
        Controller.endMyTurn()
        View.sendToClient(UserMadeHisMove(Switch()))
        mulliganButton.disable = true
      } else {
        println("Non è il mio turno!!!!!!!!!!!!!")
      }
    }
  }

  add(new VBox() {
    styleClass += "timer-container"
    children = timerPanel
  }, 0, 0)

  add(new HBox(5) {
    styleClass += "button-container"
    children = List(new Button("Submit") {
      onAction = handle {
        if(Controller.isMyTurn) {
          println("Giocato")
          timerPanel.pauseTimer()
          Controller.endMyTurn()
          Controller.playWord()
        } else {
          println("Non è il mio turno!!!!!!!!!!!!!")
        }
      }
    },
      new Button("PASS") {
        onAction = handle {
          if(Controller.isMyTurn) {
            println("Passato")
            timerPanel.pauseTimer()
            View.sendToClient(UserMadeHisMove(Pass()))
          } else {
            println("Non è il mio turno!!!!!!!!!!!!!")
          }
        }
      },
      mulliganButton,
      new Button("Get Letters Back") {
        onAction = handle {
          println("Ritiro le lettere giocate questo turno")
          Controller.collectLetters()
          BoardInteraction.collectLetters()
        }
      }
    )
  }, 0, 1)

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


