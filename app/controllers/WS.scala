package controllers

import java.time.{LocalDateTime, Duration}

import akka.actor._
import akka.event.LoggingReceive
import com.typesafe.config.Config
import controllers.{Register, Move}
import play.api.{Play, Configuration}
import play.api.mvc._
import play.api.Play.current
import akka.actor.ActorLogging

import reactivemongo.api._
import reactivemongo.bson._

import scala.concurrent.Future
import scala.util.{Success, Failure}

import scala.concurrent.ExecutionContext.Implicits.global


object MyController extends Controller {

  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
    println("session:"+request.session)
    println("headers:"+request.headers)
    println("cookies:"+request.cookies)
    println("remoteAddress:"+request.remoteAddress)
    ReceiverActor.props(out, request.headers)
  }
}

object ReceiverActor {
  def props(out: ActorRef, headers: Headers) = Props(new ReceiverActor(out, headers))
}

object Random {
  def nextId : String = scala.util.Random.alphanumeric.take(6).mkString.toLowerCase
}

class ReceiverActor(out: ActorRef, headers: Headers) extends Actor {
  var playerId = Random.nextId
  val supervisor = context.actorSelection("/user/supervisor")

  // communicate with browsers in raw text
  def receive = LoggingReceive ({
    case s: String => s.split(":") match {
      case Array("create") =>
        supervisor ! Create(Player(self, out, playerId), Random.nextId)

      case Array("connect", _) =>
        supervisor ! Register(Player(self, out, playerId), headers)

      case Array("join", gameId) =>
        supervisor ! Register(Player(self, out, playerId), headers)
        supervisor ! Join(Player(self, out, playerId), gameId)

      case Array("move", gameId, sowId) =>
        supervisor ! Move(gameId, playerId, sowId)
    }
  })

  override def postStop() = {
    supervisor ! Close(playerId)
  }
}

class SupervisorActor extends Actor with ActorLogging {
  var members : scala.collection.mutable.ArrayBuffer[Player] = scala.collection.mutable.ArrayBuffer()
  var games : scala.collection.mutable.Map[String, Game] = scala.collection.mutable.Map()

  def nbPlayers = members.size
  def nbGames = games.collect{ case (_, g: ReadyGame) => g}.size

  def receive = LoggingReceive {
    case Close(playerId) => {
      // find opponents, to inform them
      val opponents = games.collect{
        case (_, ReadyGame(_, Player(playerId, _, _), p2, _, _, _, _)) => p2
        case (_, ReadyGame(_, p1, Player(playerId, _, _), _, _, _, _)) => p1
      }
      opponents.foreach { _.out! "close"}

      members.find(p => p.playerId == playerId).foreach(p=>members.-=(p))
      members.foreach(_.out ! s"stats:$nbPlayers:$nbGames")
    }

    case msg@Register(player, headers) => {
      context.actorSelection("/user/store") ! msg
      log.debug("Register:{}", player.playerId)
      members.+=(player)
      members.foreach(_.out ! s"stats:$nbPlayers:$nbGames")
    }

    case msg @ Join(member, gameId) => {
      context.actorSelection("/user/store") ! msg
      games.get(gameId) match {
        case Some(g: PreGame) =>
          val updatedGame = g.join(member)
          games.put(gameId, updatedGame)
          updatedGame.player1.out ! ("join1")
          updatedGame.player1.out ! (s"active")
          updatedGame.player2.out ! ("join2")
          updatedGame.player2.out ! (s"passive")
        case Some(_: ReadyGame) => member.out ! "error:You can't join because there are already two players" // we can't join a ReadyGame
        case None => member.out ! "error:You can't join because this game doesn't exist"
      }

      members.foreach(_.out ! s"stats:$nbPlayers:$nbGames")
    }

    case msg @ Create(member, gameId) => {
      context.actorSelection("/user/store") ! msg
      games.put(gameId, PreGame(gameId, member))
      member.out ! (s"game:${gameId}")

      members.foreach(_.out ! s"stats:$nbPlayers:$nbGames")
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

object BSONMap {
  implicit def MapReader[V](implicit vr: BSONDocumentReader[V]): BSONDocumentReader[Map[String, V]] = new BSONDocumentReader[Map[String, V]] {
    def read(bson: BSONDocument): Map[String, V] = {
      val elements = bson.elements.map { tuple =>
        // assume that all values in the document are BSONDocuments
        tuple._1 -> vr.read(tuple._2.seeAsTry[BSONDocument].get)
      }
      elements.toMap
    }
  }

  implicit def MapWriter[V](implicit vw: BSONDocumentWriter[V]): BSONDocumentWriter[Map[String, V]] = new BSONDocumentWriter[Map[String, V]] {
    def write(map: Map[String, V]): BSONDocument = {
      val elements = map.toStream.map { tuple =>
        tuple._1 -> vw.write(tuple._2)
      }
      BSONDocument(elements)
    }
  }
}

class MongoActor extends Actor with ActorLogging {
  import BSONMap._

  // gets an instance of the driver
  // (creates an actor system)
  val uri = Play.configuration.getString("mongodb.uri").get
  val driver = new MongoDriver
  val connection = (for {
    uri <- Play.configuration.getString("mongodb.uri")
    parsedURI <- MongoConnection.parseURI(uri).toOption
  } yield driver.connection(parsedURI)).orNull

  def receive = LoggingReceive {
    case msg@Register(player, headers) => {
      val event = BSONDocument(
        "type" -> "register",
        "timestamp" -> LocalDateTime.now.toString,
        "headers" -> BSONDocument(headers.toMap.map{case (k, v) => k -> BSONArray(v)}),
        "playerId" -> player.playerId
      )
      // Gets a reference to the database "plugin"
      val db = connection("plugin")

      // Gets a reference to the collection "acoll"
      // By default, you get a BSONCollection.
      val collection = db("acoll")

      val future = collection.insert(event)

      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {
          println("successfully inserted document with lastError = " + lastError)
        }
      }
    }

    case Join(member, gameId) => {
      val event = BSONDocument(
        "type" -> "join",
        "timestamp" -> LocalDateTime.now.toString,
        "playerId" -> member.playerId,
        "gameId" -> gameId
      )
      // Gets a reference to the database "plugin"
      val db = connection("plugin")

      // Gets a reference to the collection "acoll"
      // By default, you get a BSONCollection.
      val collection = db("acoll")

      val future = collection.insert(event)

      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {
          println("successfully inserted document with lastError = " + lastError)
        }
      }
    }
    case Create(member, gameId) => {
      val event = BSONDocument(
        "type" -> "create",
        "timestamp" -> LocalDateTime.now.toString,
        "playerId" -> member.playerId,
        "gameId" -> gameId
      )
      // Gets a reference to the database "plugin"
      val db = connection("plugin")

      // Gets a reference to the collection "acoll"
      // By default, you get a BSONCollection.
      val collection = db("acoll")

      val future = collection.insert(event)

      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {
          println("successfully inserted document with lastError = " + lastError)
        }
      }
    }
    case Move(gameId, playerId, sowId) => {
      val event = BSONDocument(
        "type" -> "move",
        "timestamp" -> LocalDateTime.now.toString,
        "gameId" -> gameId,
        "playerId" -> playerId,
        "sowId" -> sowId
      )
      // Gets a reference to the database "plugin"
      val db = connection("plugin")

      // Gets a reference to the collection "acoll"
      // By default, you get a BSONCollection.
      val collection = db("acoll")

      val future = collection.insert(event)

      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {
          println("successfully inserted document with lastError = " + lastError)
        }
      }
    }
  }
}

// In Message
// Game
case class Create(member : Player, gameId: String)
case class Join(member : Player, gameId: String)
case class Move(gameId : String, playerId : String, sowId: String)
// Player
case class Register(member : Player, headers: Headers)

// Out Message
case class Close(playerId : String)
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
