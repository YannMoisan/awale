package controllers

import akka.actor._
import play.api.libs.concurrent._
import play.api.mvc._
import play.api.Play.current

object MyController extends Controller {
  lazy val supervisor = Akka.system.actorOf(Props[SupervisorActor])

  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
    ReceiverActor.props(out, supervisor)
  }
}

object ReceiverActor {
  def props(out: ActorRef, supervisor: ActorRef) = Props(new ReceiverActor(out, supervisor))
}

object Random {
  def nextId : String = scala.util.Random.alphanumeric.take(6).mkString.toLowerCase
}

class ReceiverActor(out: ActorRef, supervisor: ActorRef) extends Actor {
  var playerId = Random.nextId

  // communicate with browsers in raw text
  def receive = {
    case msg: String if msg == "create" =>
      supervisor ! Create(Player(self, out, playerId))

    case msg: String if msg.startsWith("connect") =>
      supervisor ! Register(Player(self, out, playerId))
      //supervisor ! Broadcast(s"New player $playerId : $msg")

    case msg: String if msg.startsWith("join") =>
      supervisor ! Join(Player(self, out, playerId), msg.substring(5))
    //supervisor ! Broadcast(s"New player $playerId : $msg")

    case msg: String if msg.startsWith("move") =>
      // TODO : do not register at each message
      //supervisor ! Register((self, out))
      val Array(_, gameId, sowId) = msg.split(':')

      supervisor ! Move(gameId, playerId, sowId)
  }

  override def postStop() = {
    supervisor ! Close(playerId)
  }
}

class SupervisorActor extends Actor {
  var members2 : scala.collection.mutable.ArrayBuffer[Player] = scala.collection.mutable.ArrayBuffer()
  var members : scala.collection.mutable.Map[String, Game] = scala.collection.mutable.Map()
  def nbPlayers = allPlayers.size + members2.size
  def nbGames = members.size

  def allPlayers = members2 ++ members.flatMap {
    case (_, Game(_, p1, maybeP2, _)) => maybeP2 match {
      case Some(p2) => Seq(p1, p2)
      case None => Seq(p1)
    }
  }

  def receive = {
    case Close(playerId) => {
      val maybeGame = members.find(p => p._2.player1.playerId == playerId || p._2.player2.map(_.playerId) == Some(playerId))
      maybeGame match {
        case Some((_, Game(_, p1, p2, _))) if p1.playerId == playerId => p2.get.out ! "close"
        case Some((_, Game(_, p1, p2, _))) => p1.out ! "close"
        case None =>
      }
      allPlayers.foreach(_.out ! s"Stats:$nbPlayers:$nbGames")
    }

    case Register(player) => {
      println("REgister:"+player.playerId)
      members2.+=(player)
      allPlayers.foreach(_.out ! s"Stats:$nbPlayers:$nbGames")
    }

    case Join(member, gameId) => {
      println(s"Join '$gameId'")
      val currentGame = members(gameId)
      val updatedGame = currentGame.join(member)
      members.put(gameId, updatedGame)
      updatedGame.player1.out ! ("join1")
      updatedGame.player1.out ! (s"active")
      updatedGame.player2.get.out ! ("join2")
      updatedGame.player2.get.out ! (s"passive")

      allPlayers.foreach(_.out ! s"Stats:$nbPlayers:$nbGames")

//      if (members.length == 1) {
//        members(0)._2 ! (s"gamejoin")
//      }
//      members = member :: members
    }

    case Create(member) => {
      val gameId = Random.nextId
      println(s"Create '$gameId'")
      members.put(gameId, Game(gameId, member, None, 1))
      member.out ! (s"Game:${gameId}")

      allPlayers.foreach(_.out ! s"Stats:$nbPlayers:$nbGames")

      //members = member :: members
      //members.foreach { _._2 ! (s"Register a new player:${members.length}") }
    }

    case Move(gameId, playerId, sowId) => {
      val updatedGame = members(gameId).move(sowId)
      members.put(gameId, updatedGame)
      updatedGame.player1.out ! (if (updatedGame.currentPlayer == 1) "active"+sowId else "passive")
      updatedGame.player2.get.out ! (if (updatedGame.currentPlayer == 2) "active"+sowId else "passive")

      //members(gameId).player2.get.out ! (s"$gameId-$playerId-$sowId")
      //count = count + 1
      //currentPlayer = (currentPlayer + 1) % 2
    }
  }
}

// Message
case class Create(member : Player)
case class Join(member : Player, gameId: String)
case class Register(member : Player)
case class Move(gameId : String, playerId : String, sowId: String)
case class Close(playerId : String)
case class Stats(nbPlayers: Int, nbGames: Int)

// Model
case class Player(ref: ActorRef, out: ActorRef, playerId: String)

case class Game(gameId: String, player1 : Player, player2 : Option[Player], currentPlayer : Integer) {
  def join(player: Player) = this.copy(player2 = Some(player))
  def move(sowId: String) = this.copy(currentPlayer = if (this.currentPlayer == 1) 2 else 1)
}

