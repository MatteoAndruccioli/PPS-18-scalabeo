package client.view

import scalafx.application.Platform
import scalafx.scene.layout.VBox
import scalafx.scene.text.Text

/** Classe che rappresenta la classifica della partita.
 *
 * @param users i nomi degli utenti
 */
class Leaderboard(users: List[String]) extends VBox {
  private val WIDTH = 250
  private val HEIGHT = 240
  private val _leaderboard: List[(String, Int)] = users.map(u => (u, 0))
  val rankContainer: VBox = new VBox() {
    styleClass += "player-container"
  }
  updateLeaderboard(_leaderboard)
  prefHeight = HEIGHT
  prefWidth = WIDTH
  styleClass += "leaderboard"
  children = List(
    new Text("Classifica") {
      styleClass += "title"
    },
    rankContainer
  )

  /** Metodo chiamato per aggiornare la classifica dei giocatori.
   *
   * @param ranking la classifica della partita
   */
  def updateLeaderboard(ranking: List[(String, Int)]): Unit = {
    Platform.runLater(() => {
      rankContainer.children = ranking.sortBy(u => u._2).reverse.map(u => new Text(u._1 + ": " + u._2 + "pts") {styleClass += "player-text"})
    })
  }
}
