package client.view

import scalafx.scene.layout.{ColumnConstraints, GridPane}

class LegendPanel(users: List[String]) extends GridPane {

  val leaderBoard = new Leaderboard(users)
  stylesheets = List("/style/LegendStyle.css")
  prefWidth = 250
  prefHeight = 720
  columnConstraints = List(new ColumnConstraints(250))
  add(leaderBoard, 0, 0)
  add(new Legend, 0, 1)

  def updateLeaderboard(ranking: List[(String, Int)]): Unit = {
    leaderBoard.updateLeaderboard(ranking)
  }
}
