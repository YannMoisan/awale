package controllers

import akka.actor.{ActorRef, Props}
import controllers.SupervisorActor
import play.api._
import play.libs.Akka

object Global extends GlobalSettings {

  var supervisor : ActorRef = null;

  override def onStart(app: Application) {
    supervisor = Akka.system.actorOf(Props[SupervisorActor])

  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

}
