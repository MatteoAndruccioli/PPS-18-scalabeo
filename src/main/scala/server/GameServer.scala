package server

import shared.Topic.GAME_SERVER_SEND_TOPIC
import server.GreetingToGameServer.InitGame
import akka.actor.{Actor, ActorRef}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import model._
import shared.ClientMoveAckType._
import shared.ClientToGameServerMessages.{ClientMadeMove, EndTurnUpdateAck, MatchTopicListenAck, PlayerTurnBeginAck}
import shared.GameServerToClientMessages.{ClientMoveAck, EndTurnUpdate, MatchTopicListenQuery, PlayerTurnBegins}
import shared.{ClusterScheduler, CustomScheduler, Move}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class GameServer(players : List[ActorRef], mapUsername : Map[ActorRef, String]) extends Actor {

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(GAME_SERVER_SEND_TOPIC, self)
  private val cluster = Cluster.get(context.system)
  private val scheduler: CustomScheduler = ClusterScheduler(cluster)
  private val nPlayer = players.size

  private var greetingServerRef: ActorRef = _
  private val gamePlayers = players
  private val gamePlayersUsername: Map[ActorRef, String] = mapUsername

  private val board = BoardImpl()
  private val pouch = LettersBagImpl()
  private var playersHand = mutable.Map[ActorRef, LettersHandImpl]()
  //creo le mani
  gamePlayers.foreach(p => playersHand+=(p -> LettersHandImpl.apply(mutable.ArrayBuffer(pouch.takeRandomElementFromBagOfLetters(8).get : _*))))
  private var playedWord : ArrayBuffer[BoardTile] = ArrayBuffer[BoardTile]()
  private var numberOfPlayedTileInHand = 0
  //creo il dizionario
  private val dictionaryPath: String = "/dictionary/dictionary.txt"
  private val dictionary: DictionaryImpl = new DictionaryImpl(dictionaryPath)

  private val ranking : Ranking = new RankingImpl(players)

  private var turn = 0

  //variabili ack
  private var ackTopicReceived = CounterImpl(nPlayer)
  private var ackTurn = CounterImpl(nPlayer)
  private var ackEndTurn = CounterImpl(nPlayer)

  override def receive: Receive = {
    case _: InitGame =>
      scheduler.replaceBehaviourAndStart(() => sendTopic())
      greetingServerRef = sender()
    case _: MatchTopicListenAck =>
      ackTopicReceived.increment()
      if (ackTopicReceived.isFull()) {
        scheduler.stopTask()
        ackTopicReceived.reset()
        scheduler.replaceBehaviourAndStart(() => sendTurn())
        println("STA A" + gamePlayers(turn).toString() + " IL CUI TURNO è = " + turn)
      }
    case _: PlayerTurnBeginAck =>
      ackTurn.increment()
      if (ackTurn.isFull()) {
        scheduler.stopTask()
        ackTurn.reset()
      }
    case message: ClientMadeMove => message.move match {
      case _: Move.Pass =>
        if (sender().equals(gamePlayers(turn))) {
          sender ! ClientMoveAck(PassAck())
          println("IL player " + sender() + " ha passato")
          scheduler.replaceBehaviourAndStart(() => sendUpdate())
        }
      case _: Move.TimeOut =>
        if (sender().equals(gamePlayers(turn))) {
          sender ! ClientMoveAck(TimeoutAck())
          println("IL player " + sender() + " ha fatto passare troppo tempo: TIMEOUT")
          scheduler.replaceBehaviourAndStart(() => sendUpdate())
        }
      case _: Move.Switch =>
        if(sender().equals(gamePlayers(turn))){
          if (playersHand(sender()).containsOnlyVowelsOrOnlyConsonants()){
            val nCard = playersHand(sender())._hand.size
            pouch.reinsertCardInBag(playersHand(sender())._hand)
            playersHand(sender()) = LettersHandImpl.apply(mutable.ArrayBuffer(pouch.takeRandomElementFromBagOfLetters(nCard).get : _*))
            sender ! ClientMoveAck(HandSwitchRequestAccepted(playersHand(sender())._hand))
            scheduler.replaceBehaviourAndStart(() => sendUpdate())
          } else {
            sender ! ClientMoveAck(HandSwitchRequestRefused())
          }
        }
      case message: Move.WordMove =>
        if(sender().equals(gamePlayers(turn))){
          message.word.foreach(boardTile => playedWord.insert(0,boardTile))
          println("RICEVUTA MOSSA: "+ playedWord.toString +" da "+ sender())
          board.addPlayedWord(List.concat(playedWord))
          println(board.boardTiles.toString)
          //inserire check validità parole in futuro
          if(board.checkGoodWordDirection() && dictionary.checkWords(board.getWordsFromLetters(board.takeCardToCalculatePoints()))) {
            ranking.updatePoints(sender(),board.calculateTurnPoints(board.takeCardToCalculatePoints()))
            replaceHand()
            sender ! ClientMoveAck(WordAccepted(playersHand(sender())._hand))
            scheduler.replaceBehaviourAndStart(() => sendUpdate())
          } else {
            board.clearBoardFromPlayedWords()
            sender ! ClientMoveAck(WordRefused())
          }
          playedWord.clear()
        }

      case _: EndTurnUpdateAck =>
        ackEndTurn.increment()
        if (ackEndTurn.isFull()) {
          scheduler.stopTask()
          ackEndTurn.reset()
          incrementTurn()
          scheduler.replaceBehaviourAndStart(() => sendTurn())
          println("STA A " + gamePlayers(turn).toString() + " IL CUI TURNO è = " + turn)
        }
    }
  }

  //comportamento dello scheduler
  private def sendTopic(): Unit = {
    gamePlayers.foreach(player => player ! MatchTopicListenQuery(GAME_SERVER_SEND_TOPIC, playersHand(player)._hand, gamePlayersUsername.values.toList))
  }

  private def sendTurn(): Unit = {
    mediator ! Publish(GAME_SERVER_SEND_TOPIC, PlayerTurnBegins(gamePlayers(turn)))
  }

  private def sendUpdate(): Unit = {
    val rankingTuples : ListBuffer[(String, Int)] = ListBuffer()
    for( player <- gamePlayers){
      rankingTuples.insert(0,(gamePlayersUsername(player), ranking.ranking(player)))
    }
    mediator ! Publish(GAME_SERVER_SEND_TOPIC, EndTurnUpdate(rankingTuples.toList, board.playedWord))
  }

  //metodi utilità
  private def incrementTurn(){
    turn = turn +1
  }
  private def replaceHand() : Unit = {
    for(boardTile <- playedWord){
      playersHand(sender()).playLetter(playersHand(sender())._hand.indexOf(CardImpl(boardTile.card.letter)))
      numberOfPlayedTileInHand = numberOfPlayedTileInHand + 1
    }
    val drawnTiles = pouch.takeRandomElementFromBagOfLetters(numberOfPlayedTileInHand).getOrElse(List())
    for(i <- drawnTiles.indices) playersHand(sender()).putLetter(playersHand(sender()).hand.size,CardImpl(drawnTiles(i).letter))
    numberOfPlayedTileInHand = 0
  }
}