package client.view

import scalafx.scene.layout.{BorderPane, HBox, VBox}

import scala.collection.mutable.ArrayBuffer

class BoardAndPlayerPanel(cards: ArrayBuffer[(String, Int)]) extends BorderPane {
  private val LETTER_SIZE = 60
  private val LETTER_PLACEHOLDER_VALUE = 0
  private val LETTERS_SPACING = 8
  private val board = new BoardPanel
  stylesheets = List("/style/BPStyle.css")
  styleClass += "body"

  val myHand: HBox = new HBox(LETTERS_SPACING) {
    styleClass += "my-hand"
  }

  cards.zipWithIndex.foreach(c => myHand.getChildren.add(LetterTile(LETTER_SIZE, c._1._1, c._1._2.toString, c._2, LetterStatus.inHand)))
  BoardInteraction.setMyHand(myHand)

  val opponentHandTop: HBox = new HBox(LETTERS_SPACING) {
    styleClass += "top-opponent"
    children = placeholderhand()
  }

  val opponentHandLeft: VBox = new VBox(LETTERS_SPACING) {
    styleClass += "left-opponent"
    children = placeholderhand()
  }

  val opponentHandRight: VBox = new VBox(LETTERS_SPACING) {
    styleClass += "right-opponent"
    children = placeholderhand()
  }

  center = board
  left = opponentHandLeft
  right = opponentHandRight
  top = opponentHandTop
  bottom = myHand

  private def placeholderhand(): List[LetterTile] = {
    val list = List.fill(8)(LetterTile(LETTER_SIZE, "", "", LETTER_PLACEHOLDER_VALUE, LetterStatus.placeHolder))
    list
  }

  def updateBoard(word: List[(LetterTile, Int, Int)]): Unit = {
    board.updateBoard(word)
  }
}
