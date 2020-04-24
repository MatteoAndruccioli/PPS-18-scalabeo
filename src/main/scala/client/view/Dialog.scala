package client.view

import scalafx.Includes._
import scalafx.application.Platform
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label}
import scalafx.scene.layout.{HBox, StackPane, VBox}
import scalafx.scene.paint.Color
import scalafx.stage.{Stage, StageStyle}

/** Dialog implementa una finestra di interazione per l'utente.
 *
 * @param title testo della dialog
 */
class Dialog(title: String) extends Stage(StageStyle.Undecorated) {
  val MS_BEFORE_CLOSE = 5000
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

  /** Metodo che permette di aggiungere alla dialog due bottoni, uno per confermare e uno per declinare.
   *
   * @param accept funzione da eseguire se l'utente preme il tasto di conferma
   * @param decline funzione da eseguire se l'utente preme il teasto per declinare
   * @return la dialog
   */
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

  /** Metodo che imposta un tempo limite dopo il quale la dialog si chiude.
   *
   * @param stageToClose lo stage di ScalaFX che si vuole chiudere
   * @param onCloseAction funzione chiamata alla chiusura della dialog
   * @return
   */
  def autoClose(stageToClose: Option[Stage], onCloseAction: Runnable): Dialog = {
    new Thread(() => {
      Thread.sleep(MS_BEFORE_CLOSE)
      Platform.runLater(() => {
        this.close()
        if(stageToClose != Option.empty) {
          stageToClose.get.close()
        }
        onCloseAction.run()
      })
    }).run()
    this
  }
}
