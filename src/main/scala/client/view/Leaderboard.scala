package client.view

import scalafx.application.Platform
import scalafx.scene.layout.VBox
import scalafx.scene.text.Text

class Leaderboard(users: List[String]) extends VBox {
  private val _leaderboard: List[(String, Int)] = users.map(u => (u, 0))
  val rankContainer: VBox = new VBox() {
    styleClass += "player-container"
  }
  updateLeaderboard(_leaderboard)
  prefHeight = 240
  prefWidth = 250
  styleClass += "leaderboard"
  children = List(
    new Text("Classifica") {
      styleClass += "title"
    },
    rankContainer
  )

  def updateLeaderboard(ranking: List[(String, Int)]): Unit = {
    Platform.runLater(() => {
      rankContainer.children = ranking.sortBy(u => u._2).reverse.map(u => new Text(u._1 + ": " + u._2 + "pts") {styleClass += "player-text"})
    })
  }
}
