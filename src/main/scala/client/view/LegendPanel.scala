package client.view

import scalafx.scene.layout.{ColumnConstraints, GridPane}

class LegendPanel(users: List[String]) extends GridPane {
  private val WIDTH = 250
  private val HEIGHT = 720
  private val COLUMN_CONSTRAINTS = 250
  val leaderBoard = new Leaderboard(users)
  stylesheets = List("/style/LegendStyle.css")
  prefWidth = WIDTH
  prefHeight = HEIGHT
  columnConstraints = List(new ColumnConstraints(COLUMN_CONSTRAINTS))
  add(leaderBoard, 0, 0)
  add(new Legend, 0, 1)

  def updateLeaderboard(ranking: List[(String, Int)]): Unit = {
    leaderBoard.updateLeaderboard(ranking)
  }
}
