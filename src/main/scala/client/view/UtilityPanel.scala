package client.view

import scalafx.scene.control.Button
import scalafx.scene.layout.{GridPane, HBox, VBox}
import scalafx.Includes.{handle, _}

class UtilityPanel extends GridPane {
  stylesheets = List("/style/UtilityPanel.css")
  styleClass += "body"
  prefWidth = 310
  prefHeight = 720
  val timerPanel: TimerPanel = new TimerPanel
  timerPanel.startTurn()

  val mulliganButton: Button = new Button("Mulligan") {
    onAction = handle {
      //TODO: Fare in modo che il tasto sia cliccabile solo durante il mio turno
        println("Switchato")
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
        //TODO: Fare in modo che il tasto sia cliccabile solo durante il mio turno
          println("Giocato")
      }
    },
      new Button("PASS") {
        onAction = handle {
          //TODO: Fare in modo che il tasto sia cliccabile solo durante il mio turno
            println("Passato")
        }
      },
      mulliganButton,
      new Button("Get Letters Back") {
        onAction = handle {
          println("Ritiro le lettere giocate questo turno")
        }
      }
    )
  }, 0, 1)

}


