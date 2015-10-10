package actors

import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import play.api.Play
import play.api.Play.current
import reactivemongo.api.{MongoConnection, MongoDriver}
import reactivemongo.bson.{BSONArray, BSONDocument}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

// Event store based on MongoDB
class EventStore extends Actor with ActorLogging {
  // gets an instance of the driver
  // (creates an actor system)
  val driver = new MongoDriver

  val db = (for {
    uri <- Play.configuration.getString("mongodb.uri")
    parsedURI <- MongoConnection.parseURI(uri).toOption
    conn = driver.connection(parsedURI)
    dbName <- parsedURI.db.orElse(Some("awale")) // for Heroku, db is in the URI, otherwise, it's awale
  } yield conn(dbName)).orNull

  // Gets a reference to the collection "events"
  // By default, you get a BSONCollection.
  val collection = db("events")

  def receive = LoggingReceive {
    case msg@Connect(player, headers, remoteAddress) => {
      val event = BSONDocument(
        "type" -> "connect",
        "timestamp" -> LocalDateTime.now.toString,
        "headers" -> BSONDocument(headers.toMap.map{case (k, v) => k -> BSONArray(v)}),
        "remoteAddress" -> remoteAddress,
        "playerId" -> player.playerId
      )

      val future = collection.insert(event)

      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {
          println("successfully inserted document with lastError = " + lastError)
        }
      }
    }

    case Join(gameId, player) => {
      val event = BSONDocument(
        "type" -> "join",
        "timestamp" -> LocalDateTime.now.toString,
        "playerId" -> player.playerId,
        "gameId" -> gameId
      )

      val future = collection.insert(event)

      future.onComplete {
        case Failure(e) => throw e
        case Success(lastError) => {
          println("successfully inserted document with lastError = " + lastError)
        }
      }
    }
    case Create(gameId, player) => {
      val event = BSONDocument(
        "type" -> "create",
        "timestamp" -> LocalDateTime.now.toString,
        "playerId" -> player.playerId,
        "gameId" -> gameId
      )

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
