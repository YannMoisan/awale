package controllers

import actors.ReceiverActor
import play.api.Play.current
import play.api.mvc._

object WebSocketController extends Controller {

  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
    ReceiverActor.props(out, request.headers, request.remoteAddress)
  }
}

