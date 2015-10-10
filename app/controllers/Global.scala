package controllers

import actors.{EventStore, GamesActor, PlayersActor}
import akka.actor.Props
import play.api._
import play.libs.Akka

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application start...")
    Logger.info("Create top-level actors")
    Akka.system.actorOf(Props[PlayersActor], "players")
    Akka.system.actorOf(Props[GamesActor], "games")
    Akka.system.actorOf(Props[EventStore], "store")
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

}
