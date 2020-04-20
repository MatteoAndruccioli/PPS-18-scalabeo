package client.view

import client.controller.Messages.ViewToClientMessages.ChatMessage
import scalafx.scene.control.{Button, ScrollPane, TextField}
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.scene.layout.{BorderPane, HBox, Priority}
import scalafx.scene.text.{Text, TextFlow}
import scalafx.Includes._

class ChatPanel extends BorderPane {
  val CHAT_HEIGHT = 430
  val CHAT_WIDTH = 200
  val SEND_STRING = "Invia"
  val SEND_BUTTON_SIZE = 60
  prefHeight = CHAT_HEIGHT
  prefWidth = CHAT_WIDTH

  val chatBox: TextFlow = new TextFlow() {
    styleClass += "chat"
  }

  val sendButton: Button = new Button(SEND_STRING) {
    minWidth = SEND_BUTTON_SIZE
    onAction = handle {
      if(textField.text.value.trim != "") {
        View.sendToClient(ChatMessage(textField.text.value))
        showInChat("Tu", textField.text.value)
        textField.clear()
      }
    }
  }

  val textField: TextField = new TextField() {
    hgrow = Priority.Always
    onKeyPressed = (event: KeyEvent) => {
      event.code match {
        case KeyCode.Enter =>
          sendButton.fire()
        case _ =>
      }
    }
  }

  val chatScroll: ScrollPane = new ScrollPane() {
    styleClass += "chat-scroll"
    fitToWidth = true
    fitToHeight = true
    hbarPolicy = ScrollPane.ScrollBarPolicy.Never
    vbarPolicy = ScrollPane.ScrollBarPolicy.AsNeeded
    vvalue.bind(chatBox.heightProperty())
    content = chatBox
  }

  center = chatScroll

  bottom = new HBox() {
    children = List(
      textField,
      sendButton
    )
  }

  def showInChat(sender: String, message: String): Unit = {
    val text = new Text(sender +": " + message + "\n"){
      styleClass += "message"
    }
    chatBox.getChildren.add(text)
  }

  def showEventMessage(message: String): Unit = {
    val text = new Text("Server: " + message + "\n") {
      styleClass += "event-message"
    }
    chatBox.getChildren.add(text)
  }

}
