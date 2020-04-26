package client.view

import scalafx.scene.layout.{ColumnConstraints, GridPane}

/** Pannello che contiene la classifica e la legenda.
 *
 * @param users i nomi degli utenti
 */
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

  /** Metodo chiamato per aggiornare la classifica dei giocatori.
   *
   * @param ranking la classifica della partita
   */
  def updateLeaderboard(ranking: List[(String, Int)]): Unit = {
    leaderBoard.updateLeaderboard(ranking)
  }
}
