package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    // TODO add log
    Ok(views.html.ws())
  }

  def index2(id: String) = Action {
    Ok(views.html.ws())
  }

}