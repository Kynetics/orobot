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

import java.util.UUID
import javax.inject.{ Inject, Named }

import akka.actor.{ ActorRef, ActorSystem }
import akka.pattern.ask
import akka.util.Timeout
import com.google.common.net.HttpHeaders
import com.mohiva.play.silhouette.api.{ Environment, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import elastic.json._
import json.Bad
import model.MqttMessageEvent
import models.User
import no.samordnaopptak.apidoc.ApiDoc
import persistence.RobotActorPersistence.State
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.{ Configuration, Logger }
import protocol.RobotActor.Get
import protocol.RobotRegistryActor.{ EventDetail, EventsList, RobotEvents, EventDetailResult }
import protocol.RobotRegistrySupervisor.Cmd4Registry
import protocol.RobotsSupervisor.Cmd4Robot
import utils.PaginationUtil

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class EventController @Inject() (
  configuration: Configuration,
  @Named("orobotplay") orobotplay: ActorRef,
  val system: ActorSystem,
  val messagesApi: MessagesApi,
  val env: Environment[User, JWTAuthenticator])(implicit ec: ExecutionContext)
    extends Silhouette[User, JWTAuthenticator]
    with ORobotErrorsController
    with RobotEventJson
    with MqttMessageEventJson {

  @ApiDoc(doc = """
    GET /api/events/list

    PARAMETERS
      page:Int(query,optional) <- page
      size:Int(query,optional) <- number of items per page

    DESCRIPTION
      Lists events

    RESULT
      200: Array RobotEventSummary
      401: Any <- Unauthorized

    RobotEventSummary: model.RobotEventSummary
      header: String
      timestampRec: Long
      payload: JsValue

    JsValue: !
      priority: Int
      messageUuid: String
      robotId: String
      timestamp: Long

                """)
  def listEvents(page: Int, size: Int) = SecuredAction.async { implicit request =>
    Logger.info("Listing events...")
    implicit val timeout: Timeout = 5.seconds
    (orobotplay ? Cmd4Registry(EventsList(page, size))) map {
      case result: protocol.RobotRegistryActor.EventListResult =>
        val pu = new PaginationUtil(page, size, result.result.total, "/api/events/list")
        Ok(Json.toJson(result.result.items.toSeq.seq)).withHeaders(X_TOTAL_COUNT -> result.result.total.toString, HttpHeaders.LINK -> pu.paginationHttpHeaders)
      case _ => InternalServerError(toJson(Bad(message = "oops, something went wrong")))
    }
  }

  @ApiDoc(doc = """
    GET /api/events/robot/{robotId}

    PARAMETERS
      robotId:String <- The robot ID

    DESCRIPTION
      get the Robot events

    RESULT
      200: Array RobotEventSummary
      401: Any <- Unauthorized

                """)
  def getRobotEvents(robotId: String, page: Int, size: Int) = SecuredAction.async { implicit request =>
    Logger.info(s"Getting events for robot $robotId ...")
    implicit val timeout: Timeout = 5.seconds
    val robotUuid = UUID.fromString(robotId)
    (orobotplay ? Cmd4Registry(RobotEvents(robotUuid, page, size))) map {
      case result: protocol.RobotRegistryActor.RobotEventsResult =>
        val pu = new PaginationUtil(page, size, result.result.total, s"/api/events/robot/$robotId")
        Ok(Json.toJson(result.result.items.toSeq.seq)).withHeaders(X_TOTAL_COUNT -> result.result.total.toString, HttpHeaders.LINK -> pu.paginationHttpHeaders)
      case _ => InternalServerError(toJson(Bad(message = "oops, something went wrong")))
    }
  }

  @ApiDoc(doc = """
    GET /api/events/{eventId}

    PARAMETERS
      eventId:String <- The message ID

    DESCRIPTION
      get the Event detail

    RESULT
      200: Event
      401: Any <- Unauthorized

    Event: model.MqttMessageEvent
      header: String
      timestampRec: Long
      payload: JsValueDetail

    JsValueDetail: !
      priority: Int
      messageUuid: String
      robotId: String
      timestamp: Long
      ...
                """)
  def getEvent(eventId: String) = SecuredAction.async { implicit request =>
    Logger.info(s"Getting event $eventId ...")
    implicit val timeout: Timeout = 5.seconds
    (orobotplay ? Cmd4Registry(EventDetail(UUID.fromString(eventId)))) map {
      case edr: EventDetailResult => {
        edr.result match {
          case Some(ed) => Ok(toJson(edr.result.get))
          case None => NotFound(toJson(Bad(code = Some(NOT_FOUND), message = "Not found")))
        }
      }
      case _ => InternalServerError(toJson(Bad(message = "oops, something went wrong")))
    }
  }

}
