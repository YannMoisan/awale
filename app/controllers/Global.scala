package controllers

import akka.actor.{ActorRef, Props}
import controllers.SupervisorActor
import play.api._
import play.libs.Akka

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Akka.system.actorOf(Props[SupervisorActor], "supervisor")
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

}
