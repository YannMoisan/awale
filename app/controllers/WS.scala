package controllers

import java.time.{LocalDateTime, Duration}

import akka.actor._
import akka.event.LoggingReceive
import play.api.libs.concurrent._
import play.api.mvc._
import play.api.Play.current
import akka.actor.ActorLogging

object MyController extends Controller {
  // var for testing purpose
  //var supervisor = Akka.system.actorOf(Props[SupervisorActor])

  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
    ReceiverActor.props(out, Global.supervisor)
  }
}

object ReceiverActor {
  def props(out: ActorRef, supervisor: ActorRef) = Props(new ReceiverActor(out, supervisor))
}

object Random {
  def nextId : String = { val id = scala.util.Random.alphanumeric.take(6).mkString.toLowerCase
    println(id)
    id}
}

class ReceiverActor(out: ActorRef, supervisor: ActorRef) extends Actor {
  var playerId = Random.nextId

  // communicate with browsers in raw text
  def receive = LoggingReceive ({
    case msg: String if msg == "create" =>
      println("create:"+playerId)
      println(supervisor)
      supervisor ! Create(Player(self, out, playerId))

    case msg: String if msg.startsWith("connect") =>
      supervisor ! Register(Player(self, out, playerId))

    case msg: String if msg.startsWith("join") =>
      supervisor ! Register(Player(self, out, playerId))
      supervisor ! Join(Player(self, out, playerId), msg.substring(5))

    case msg: String if msg.startsWith("move") =>
      // TODO : do not register at each message
      //supervisor ! Register((self, out))
      val Array(_, gameId, sowId) = msg.split(':')

      supervisor ! Move(gameId, playerId, sowId)
  })

  override def postStop() = {
    supervisor ! Close(playerId)
  }
}

class SupervisorActor extends Actor with ActorLogging {
  var members : scala.collection.mutable.ArrayBuffer[Player] = scala.collection.mutable.ArrayBuffer()
  var games : scala.collection.mutable.Map[String, Game] = scala.collection.mutable.Map()
  def nbPlayers = members.size
  def nbGames = games.map{ case(_, Game(_, p1, p2, _, _, _, _)) => if (p2.isDefined) 1 else 0}.sum

  def receive = LoggingReceive {
    case Close(playerId) => {
      val maybeGame = games.find(p => p._2.player1.playerId == playerId || p._2.player2.map(_.playerId) == Some(playerId))
      maybeGame match {
        case Some((_, Game(_, p1, p2, _, _, _, _))) if p1.playerId == playerId => p2.get.out ! "close"
        case Some((_, Game(_, p1, p2, _, _, _, _))) => p1.out ! "close"
        case None =>
      }
      members.find(p => p.playerId == playerId).foreach(p=>members.-=(p))
      members.foreach(_.out ! s"Stats:$nbPlayers:$nbGames")
    }

    case Register(player) => {
      log.debug("Register:{}", player.playerId)
      members.+=(player)
      members.foreach(_.out ! s"Stats:$nbPlayers:$nbGames")
    }

    case Join(member, gameId) => {
      val currentGame = games(gameId)
      val updatedGame = currentGame.join(member)
      games.put(gameId, updatedGame)
      updatedGame.player1.out ! ("join1")
      updatedGame.player1.out ! (s"active")
      updatedGame.player2.get.out ! ("join2")
      updatedGame.player2.get.out ! (s"passive")

      members.foreach(_.out ! s"Stats:$nbPlayers:$nbGames")

    }

    case Create(member) => {
      log.debug("supervisor.create{}")
      println("supervisor.create")
      val gameId = Random.nextId
      games.put(gameId, Game(gameId, member, None, 1, LocalDateTime.now, Duration.ZERO, Duration.ZERO))
      member.out ! (s"Game:${gameId}")

      members.foreach(_.out ! s"Stats:$nbPlayers:$nbGames")
    }

    case Move(gameId, playerId, sowId) => {
      val updatedGame = games(gameId).move(sowId)
      games.put(gameId, updatedGame)
      val durations = s":${updatedGame.p1Duration}:${updatedGame.p2Duration}"
      updatedGame.player1.out ! (if (updatedGame.currentPlayer == 1) "active:"+sowId+durations else "passive" + durations)
      updatedGame.player2.get.out ! (if (updatedGame.currentPlayer == 2) "active:"+sowId+durations else "passive" + durations)
    }
  }
}

// In Message
case class Create(member : Player)
case class Join(member : Player, gameId: String)
case class Register(member : Player)
case class Move(gameId : String, playerId : String, sowId: String)

// Out Message
case class Close(playerId : String)
case class Stats(nbPlayers: Int, nbGames: Int)

// Model
case class Player(ref: ActorRef, out: ActorRef, playerId: String)

case class Game(gameId: String, player1 : Player, player2 : Option[Player], currentPlayer : Integer, lastDate : LocalDateTime, p1Duration : Duration, p2Duration : Duration) {
  def join(player: Player) = this.copy(player2 = Some(player))
  def move(sowId: String) = {
    val curLDT = LocalDateTime.now
    this.copy(
      currentPlayer = if (this.currentPlayer == 1) 2 else 1,
      p1Duration = if (this.currentPlayer == 1) {p1Duration.plus(Duration.between(lastDate, curLDT))} else p1Duration,
      p2Duration = if (this.currentPlayer == 2) {p2Duration.plus(Duration.between(lastDate, curLDT))} else p2Duration,
      lastDate = curLDT)
  }
}

