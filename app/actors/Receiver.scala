package actors

import java.time.{Duration, LocalDateTime}

import akka.actor.{ActorLogging, Actor, Props, ActorRef}
import akka.event.LoggingReceive
import play.api.mvc.Headers

object ReceiverActor {
  def props(out: ActorRef, headers: Headers, remoteAddress: String) = Props(new ReceiverActor(out, headers, remoteAddress))
}

object Random {
  def nextId : String = scala.util.Random.alphanumeric.take(6).mkString.toLowerCase
}

class ReceiverActor(out: ActorRef, headers: Headers, remoteAddress: String) extends Actor {
  var playerId = Random.nextId
  val games = context.actorSelection("/user/games")
  val players = context.actorSelection("/user/players")

  // communicate with browsers in raw text
  def receive = LoggingReceive ({
    case s: String => s.split(":") match {
      case Array("create") =>
        games ! Create(Random.nextId, Player(self, out, playerId))

      case Array("connect", _) =>
        players ! Connect(Player(self, out, playerId), headers, remoteAddress)

      case Array("join", gameId) =>
        players ! Connect(Player(self, out, playerId), headers, remoteAddress)
        games ! Join(gameId, Player(self, out, playerId))

      case Array("move", gameId, sowId) =>
        games ! Move(gameId, playerId, sowId)
    }
  })

  // properly handle unexpected deconnection : close tab, …
  override def postStop() = {
    players ! Disconnect(playerId)
  }
}

class GamesActor extends Actor with ActorLogging {
  var games : scala.collection.mutable.Map[String, Game] = scala.collection.mutable.Map()

  def nbGames = games.collect{ case (_, g: ReadyGame) => g}.size

  def receive = LoggingReceive {
    case msg@AskOpponent(playerId) =>
      val opponents = games.collect{
        case (_, ReadyGame(_, Player(playerId, _, _), p2, _, _, _, _)) => p2
        case (_, ReadyGame(_, p1, Player(playerId, _, _), _, _, _, _)) => p1
      }
      sender() ! ReplyOpponent(opponents)

    case msg @ Join(gameId, player) => {
      context.actorSelection("/user/store") ! msg
      games.get(gameId) match {
        case Some(g: PreGame) =>
          val updatedGame = g.join(player)
          games.put(gameId, updatedGame)
          updatedGame.player1.out ! ("join1")
          updatedGame.player2.out ! ("join2")

          val durations = s":${updatedGame.p1Duration.getSeconds}:${updatedGame.p2Duration.getSeconds}"
          updatedGame.activePlayer.out ! "active:" + ":" + durations
          updatedGame.passivePlayer.out ! "passive" + durations
        case Some(_: ReadyGame) => player.out ! "error:You can't join because there are already two players" // we can't join a ReadyGame
        case None => player.out ! "error:You can't join because this game doesn't exist"
      }
      context.actorSelection("/user/players") ! BroadcastMetrics
    }

    case msg @ Create(gameId, player) => {
      context.actorSelection("/user/store") ! msg
      games.put(gameId, PreGame(gameId, player))
      player.out ! (s"game:${gameId}")
    }

    case msg @ Move(gameId, playerId, sowId) => {
      context.actorSelection("/user/store") ! msg
      games(gameId) match {
        case game: ReadyGame =>
          val updatedGame = game.move(sowId)
          games.put(gameId, updatedGame)

          val durations = s":${updatedGame.p1Duration.getSeconds}:${updatedGame.p2Duration.getSeconds}"
          updatedGame.activePlayer.out ! "active:" + sowId + durations
          updatedGame.passivePlayer.out ! "passive" + durations
        case _: PreGame => // we can't move a PreGame
      }
    }
  }
}

class PlayersActor extends Actor with ActorLogging {
  var players : scala.collection.mutable.ArrayBuffer[Player] = scala.collection.mutable.ArrayBuffer()

  def nbPlayers = players.size

  def receive = LoggingReceive {
    case Disconnect(playerId) => {
      players.find(p => p.playerId == playerId).foreach(p=>players.-=(p))
      self ! BroadcastMetrics

      // find opponents, to inform them
      context.actorSelection("/user/games") ! AskOpponent(playerId)
    }

    case ReplyOpponent(players) =>
      players.foreach { _.out! "close"}

    case msg@Connect(player, headers, remoteAddress) => {
      context.actorSelection("/user/store") ! msg
      log.debug("Connect:{}", player.playerId)
      players.+=(player)
      self ! BroadcastMetrics
    }
    case BroadcastMetrics =>
      context.actorSelection("/user/games") ! AskNbGames
      
    case ReplyNbGames(nbGames) =>  
      players.foreach(_.out ! s"stats:$nbPlayers:$nbGames")
  }
}


// In Message
// Game
case class Create(gameId: String, player: Player)
case class Join(gameId: String, player: Player)
case class Move(gameId : String, playerId : String, sowId: String)

// Player
case class Connect(player : Player, headers: Headers, remoteAddress: String)

case class AskOpponent(playerId: String)
case class ReplyOpponent(players: Iterable[Player])

case object AskNbGames
case class ReplyNbGames(nb : Integer)

case object BroadcastMetrics

// Out Message
case class Disconnect(playerId : String)
case class Stats(nbPlayers: Int, nbGames: Int)

// Model
case class Player(ref: ActorRef, out: ActorRef, playerId: String)


// depending of the state, variable should be filled or not. so let put that in the design
sealed trait Game
case class PreGame(gameId: String, player1 : Player) extends Game {
  def join(player2: Player) = ReadyGame(gameId, player1, player2, 1, LocalDateTime.now, Duration.ZERO, Duration.ZERO)
}

case class ReadyGame(gameId: String, player1 : Player, player2 : Player,
                     currentPlayer : Integer, lastDate : LocalDateTime, p1Duration : Duration, p2Duration : Duration) extends Game {
  def move(sowId: String) = {
    val curLDT = LocalDateTime.now
    this.copy(
      currentPlayer = if (this.currentPlayer == 1) 2 else 1,
      p1Duration = if (this.currentPlayer == 1) {p1Duration.plus(Duration.between(lastDate, curLDT))} else p1Duration,
      p2Duration = if (this.currentPlayer == 2) {p2Duration.plus(Duration.between(lastDate, curLDT))} else p2Duration,
      lastDate = curLDT)
  }
  def activePlayer  = if (this.currentPlayer == 1) player1 else player2
  def passivePlayer = if (this.currentPlayer != 1) player1 else player2
}
