/*
* Copyright 2015 Kynetics SRL
*
* This file is part of orobot.
*
* orobot is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* orobot is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with orobot.  If not, see <http://www.gnu.org/licenses/>.
*/
package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import play.api.Play.current
import play.api.mvc.{ AnyContentAsEmpty, Request, WebSocket }
import websocket.actors.ORobotWebSocketActor
import com.mohiva.play.silhouette.api.{ Environment, Silhouette }
import models.User
import play.api.i18n.MessagesApi
import websocket.protocol._

import scala.concurrent.{ ExecutionContext, Future }

class WebSocketsController @Inject() (val messagesApi: MessagesApi, val env: Environment[User, JWTAuthenticator])(implicit ec: ExecutionContext)
    extends Silhouette[User, JWTAuthenticator] {

  def wsConnect(token: String) = WebSocket.tryAcceptWithActor[WSMessage, WSMessage] { request =>
    implicit val req = Request(request.copy(headers = request.headers.add("X-Auth-Token" -> token)), AnyContentAsEmpty)
    SecuredRequestHandler { securedRequest =>
      Future.successful(HandlerResult(Ok, Option(securedRequest.identity)))
    }.map {
      case HandlerResult(r, Some(user)) => Right(ORobotWebSocketActor.props(user) _)
      case HandlerResult(r, None) => Left(Unauthorized)
    }
  }
}
