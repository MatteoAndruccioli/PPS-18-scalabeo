package client.controller

object Messages {
  sealed trait ViewToClientMessages
  object ViewToClientMessages {
    //message sent after user entered his username
    case class UsernameChosen(username: String) extends ViewToClientMessages
    //message specifying the request to join the queue
    case class JoinQueue() extends ViewToClientMessages
    //player answer to ready to join request
    case class UserReadyToJoin(ready:Boolean) extends ViewToClientMessages
    //player wants to play again
    case class PlayAgain(userWantsToPlay:Boolean) extends ViewToClientMessages
    //player exited
    case class UserExited() extends  ViewToClientMessages
  }
}
