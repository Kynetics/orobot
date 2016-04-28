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

import java.awt.image.renderable.RenderableImage
import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, File }
import java.util.{ Base64, UUID }
import java.util.zip.{ GZIPInputStream, GZIPOutputStream }
import javax.imageio.ImageIO
import javax.inject.{ Inject, Named }

import akka.actor.{ ActorRef, ActorSystem }
import akka.pattern.ask
import akka.util.Timeout
import com.google.common.net.HttpHeaders
import com.mohiva.play.silhouette.api.{ Environment, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import elastic.json.{ AddressJson, CustomerJson, RobotJson }
import json._
import model.{ Address, Customer, Robot }
import models.User
import no.samordnaopptak.apidoc.ApiDoc
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.{ Configuration, Logger }
import protocol.RobotActor.{ Create, Get, Initialized }
import protocol.RobotsSupervisor.Cmd4Robot
import protocol.RobotRegistryActor.RobotsList
import protocol.RobotRegistrySupervisor.Cmd4Registry
import utils.PaginationUtil

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import persistence.RobotActorPersistence.State
import net.ceedubs.ficus.Ficus._

class RobotController @Inject() (
  configuration: Configuration,
  @Named("orobotplay") orobotplay: ActorRef,
  val system: ActorSystem,
  val messagesApi: MessagesApi,
  val env: Environment[User, JWTAuthenticator])(implicit ec: ExecutionContext)
    extends Silhouette[User, JWTAuthenticator]
    with ORobotErrorsController
    with CustomerJson
    with AddressJson
    with RobotJson
    with RobotSummaryJson
    with RobotStateJson
    with RobotDetailJson {

  val customerForm = Form(
    mapping(
      "codiceFiscale" -> nonEmptyText,
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "email" -> nonEmptyText,
      "address" -> mapping(
        "street" -> nonEmptyText,
        "cap" -> nonEmptyText,
        "city" -> nonEmptyText,
        "pv" -> nonEmptyText
      )(Address.apply)(Address.unapply),
      "phone" -> nonEmptyText
    )(Customer.apply)(Customer.unapply)
  )

  @ApiDoc(doc = """
    POST /api/robots/create

    PARAMETERS
      body: Customer <- customer data

    DESCRIPTION
      create customer and robot

    Address: model.Address
      street: String
      cap: Int
      city: String
      pv: String

    Customer: model.Customer
      codiceFiscale: String
      firstName: String
      lastName: String
      email: String
      address: Address
      phone: Int
                """)
  def createRobot = SecuredAction.async(parse.json) { implicit request =>
    Logger.info("Creating robot...")
    implicit val timeout: Timeout = 5.seconds
    Logger.info(request.body.toString())
    val json = request.body.validate[Customer]
    Logger.info(json.toString)
    execute(json) { customer =>
      val uuid = UUID.randomUUID()
      (orobotplay ? Cmd4Robot(uuid, Create(customer))) map {
        case Initialized(robot: Robot) => Created(toJson(robot))
        case _ => InternalServerError(toJson(Bad(message = "oops, something went wrong")))
      }
    }
  }

  @ApiDoc(doc = """
    GET /api/robots/list

    PARAMETERS
      page:Int(query,optional) <- page
      size:Int(query,optional) <- number of items per page
      order:Int(query,optional) <- ordering

    DESCRIPTION
      Lists robots
                """)
  def listRobots(page: Int, size: Int, order: Option[Seq[Int]]) = SecuredAction.async { implicit request =>
    Logger.info("Listing robot...")
    implicit val timeout: Timeout = 5.seconds
    (orobotplay ? Cmd4Registry(RobotsList(page, size, order))) map {
      case r: protocol.RobotRegistryActor.RobotListResult =>
        val pu = new PaginationUtil(page, size, r.total, "/api/robots/list")
        Ok(Json.toJson(r.items)).withHeaders(X_TOTAL_COUNT -> r.total.toString, HttpHeaders.LINK -> pu.paginationHttpHeaders)
      case _ => InternalServerError(toJson(Bad(message = "oops, something went wrong")))
    }
  }

  @ApiDoc(doc = """
    GET /api/robots/{robotId}

    PARAMETERS
      robotId:String <- The robot ID

    DESCRIPTION
      get the Robot details

    RESULT
      200: State
      401: Any <- Unauthorized

    State: persistence.RobotActorPersistence.State
      uuid: String
      customer: Customer
      configured: Boolean
      missedAlive: Int
      mqttConnected: Boolean
      locationMap: String
      removed: Boolean
                """)
  def getRobot(robotId: String) = SecuredAction.async { implicit request =>
    Logger.info(s"Getting robot $robotId ...")
    implicit val timeout: Timeout = 5.seconds
    (orobotplay ? Cmd4Robot(UUID.fromString(robotId), Get)) map {
      case s: State => {
        s.customer match {
          case Some(c) => {
            val rtcSocketUrl = configuration.underlying.as[String]("easyrtc.socketUrl")
            Ok(toJson(RobotDetail(s, rtcSocketUrl)))
          }
          case None => NotFound(toJson(Bad(code = Some(NOT_FOUND), message = "Not found")))
        }
      }
      case _ => InternalServerError(toJson(Bad(message = "oops, something went wrong")))
    }
  }

  @ApiDoc(doc = """
    GET /api/getMap/{mapname}

    PARAMETERS
      mapname:String <- The map name

    DESCRIPTION
      get the location map

                """)
  def getMap(mapname: String) = SecuredAction { implicit request =>
    Logger.info(s"Getting map $mapname ...")
    val mapsFolder = configuration.underlying.as[String]("robot.locationMap.folder")
    val mapFile = s"$mapsFolder${File.separator}$mapname.png"
    Logger.info(s"Retrieve map file $mapFile")
    if (new File(mapFile).exists()) {
      val baos = new ByteArrayOutputStream
      ImageIO.write(ImageIO.read(new File(mapFile)), "png", baos)
      val imgOut = Base64.getEncoder.encodeToString(baos.toByteArray)
      Ok(imgOut).as("image/png")
    } else InternalServerError(toJson(Bad(message = "File not found")))
  }
}
