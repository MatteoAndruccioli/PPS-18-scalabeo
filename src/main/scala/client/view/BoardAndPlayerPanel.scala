package client.view

import scalafx.scene.layout.{BorderPane, HBox, VBox}

import scala.collection.mutable.ArrayBuffer

class BoardAndPlayerPanel(cards: ArrayBuffer[(String, Int)]) extends BorderPane {

  private val board = new BoardPanel
  stylesheets = List("/style/BPStyle.css")
  styleClass += "body"

  val myHand: HBox = new HBox(8) {
    styleClass += "my-hand"
  }
  cards.zipWithIndex.foreach(c => myHand.getChildren.add(LetterTile(60, c._1._1, c._1._2.toString, c._2, LetterStatus.inHand)))
  BoardInteraction.setMyHand(myHand)

  val opponentHandTop: HBox = new HBox(8) {
    styleClass += "top-opponent"
    children = List(
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder)
    )
  }

  val opponentHandLeft: VBox = new VBox(8) {
    styleClass += "left-opponent"
    children = List(
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder)
    )
  }

  val opponentHandRight: VBox = new VBox(8) {
    styleClass += "right-opponent"
    children = List(
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder),
      LetterTile(60, "", "", 0, LetterStatus.placeHolder)
    )
  }
  center = board
  left = opponentHandLeft
  right = opponentHandRight
  top = opponentHandTop
  bottom = myHand
}
