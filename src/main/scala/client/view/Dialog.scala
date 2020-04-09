package client.view

import scalafx.Includes._
import scalafx.application.Platform
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label}
import scalafx.scene.layout.{HBox, StackPane, VBox}
import scalafx.scene.paint.Color
import scalafx.stage.{Stage, StageStyle}

class Dialog(title: String) extends Stage(StageStyle.Undecorated) {
  initStyle(StageStyle.Transparent)
  centerOnScreen()
  requestFocus()
  val elementsContainer: VBox = new VBox {
    padding = Insets(10)
    alignment = Pos.Center
    children = Seq(
      new Label(title) {
        styleClass += "title"
      }
    )
  }
  scene = new Scene {
    fill = Color.Transparent
    stylesheets_=(List("/style/DialogStyle.css"))
    root = new StackPane {
      styleClass += "dialog"
      children = elementsContainer
    }
  }

  def addYesNoButtons(accept: Runnable, decline: Runnable): Dialog = {
    val buttons: HBox = new HBox {
      alignment = Pos.Center
      margin = Insets(20, 0, 0, 0)
      children = Seq(
        new Button("Yes") {
          styleClass = List("menu-button", "accept-button")
          onAction = handle {
            accept.run()
            close()
          }
        },
        new Button("No") {
          styleClass = List("menu-button", "decline-button")
          onAction = handle {
            decline.run()
            close()
          }
        }
      )
    }
    elementsContainer.children.add(buttons)
    this
  }

  def autoClose(stageToClose: Option[Stage]): Dialog = {
    new Thread(() => {
      Thread.sleep(5000)
      Platform.runLater(() => {
        this.close()
        if(stageToClose != Option.empty) {
          stageToClose.get.close()
        }
      })
    }).run()
    this
  }
}
