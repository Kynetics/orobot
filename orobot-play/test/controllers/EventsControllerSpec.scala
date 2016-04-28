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

import java.io.File
import java.util.UUID

import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.test._
import elastic.json.{ AddressJson, CustomerJson }
import org.specs2.mock.Mockito
import org.specs2.specification.BeforeAfterAll
import play.api.Application
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import utils.OrobotTestUtils
import java.time.{ Clock, Instant }

import org.apache.commons.io.FileUtils

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class EventsControllerSpec extends PlaySpecification
    with BeforeAfterAll
    with Mockito
    with CustomerJson
    with AddressJson {

  val ts = new OrobotTestUtils(1886)

  val messageIdFP = UUID.randomUUID()
  val messageIdDP = UUID.randomUUID()
  val messageIdRP = UUID.randomUUID()
  val messageIdBL = UUID.randomUUID()

  override def beforeAll(): Unit = {
    ts.setup()
  }

  override def afterAll(): Unit = {
    ts.clean(false)
  }

  def getController(application: Application) = application.injector.instanceOf[RobotController]

  "EventsController" should {

    "list and search events successfully" in new ControllersTestCtx {
      override def testSetting: OrobotTestUtils = ts
      new WithApplication(application) {

        val fakeRequestForCreateRobot =
          FakeRequest(POST, "")
            .withAuthenticator[JWTAuthenticator](identity.loginInfo)
            .withJsonBody(Json.parse(ts.customerJson)
            )

        Thread.sleep(3000)

        val result: Future[Result] = call(getController(app).createRobot, fakeRequestForCreateRobot)
        println(s"result=${contentAsJson(result)}")
        status(result) must equalTo(CREATED)

        Thread.sleep(3000)

        val robotId = contentAsJson(result).\("id").as[String]

        val robot = application.actorSystem.actorSelection(s"/user/orobot/robots/$robotId")
        robot ! mqtt.protocol.Configure(UUID.fromString(robotId), ts.imageToTxt("map.png"))
        Thread.sleep(5000)

        robot ! mqtt.protocol.FallenPerson(Some(mqtt.protocol.PriorityHigh), messageIdFP, UUID.fromString(robotId), 1.0, 1.0, Instant.now(Clock.systemUTC()).toEpochMilli)
        Thread.sleep(2000)
        robot ! mqtt.protocol.DetectedPerson(Some(mqtt.protocol.PriorityNormal), messageIdDP, UUID.fromString(robotId), 1.0, 1.0, 1, Instant.now(Clock.systemUTC()).toEpochMilli)
        Thread.sleep(2000)
        robot ! mqtt.protocol.RecognizedPerson(Some(mqtt.protocol.PriorityNormal), messageIdRP, UUID.fromString(robotId), ts.customer.firstName, 1, Instant.now(Clock.systemUTC()).toEpochMilli)
        Thread.sleep(2000)
        robot ! mqtt.protocol.BatteryLevel(Some(mqtt.protocol.PriorityHigh), messageIdBL, UUID.fromString(robotId), Instant.now(Clock.systemUTC()).toEpochMilli)

        Thread.sleep(5000)

        val fakeRequestEvents = FakeRequest(GET, "").withAuthenticator[JWTAuthenticator](identity.loginInfo)
        val resultListEvents: Future[Result] = call(application.injector.instanceOf[EventController].listEvents(1, 20), fakeRequestEvents)
        println(s"resultListEvents=${contentAsJson(resultListEvents)}")
        println(s"headers=${headers(resultListEvents)}")
        status(resultListEvents) must equalTo(OK)
        contentType(resultListEvents) must beSome("application/json")
        header("X-Total-Count", resultListEvents) must beSome("4")
        Await.result(resultListEvents, 5.seconds)

        val resultListEventsRobot: Future[Result] = call(application.injector.instanceOf[EventController].getRobotEvents(robotId, 1, 20), fakeRequestEvents)
        println(s"resultListEventsRobot=${contentAsJson(resultListEventsRobot)}")
        println(s"headers=${headers(resultListEventsRobot)}")
        status(resultListEventsRobot) must equalTo(OK)
        contentType(resultListEventsRobot) must beSome("application/json")
        header("X-Total-Count", resultListEventsRobot) must beSome("4")
        Await.result(resultListEventsRobot, 5.seconds)

        Thread.sleep(2000)
        val getFP: Future[Result] = call(application.injector.instanceOf[EventController].getEvent(messageIdFP.toString), fakeRequestEvents)
        println(s"getFPResult=${contentAsJson(getFP)}")
        status(getFP) must equalTo(OK)
        contentType(getFP) must beSome("application/json")
        Await.result(getFP, 5.seconds)
        Thread.sleep(2000)
        val getDP: Future[Result] = call(application.injector.instanceOf[EventController].getEvent(messageIdDP.toString), fakeRequestEvents)
        println(s"getDPResult=${contentAsJson(getDP)}")
        status(getDP) must equalTo(OK)
        contentType(getDP) must beSome("application/json")
        Await.result(getDP, 5.seconds)
        Thread.sleep(2000)
        val getRP: Future[Result] = call(application.injector.instanceOf[EventController].getEvent(messageIdRP.toString), fakeRequestEvents)
        println(s"getRPResult=${contentAsJson(getRP)}")
        status(getRP) must equalTo(OK)
        contentType(getRP) must beSome("application/json")
        Await.result(getRP, 5.seconds)
        Thread.sleep(2000)
        val getBL: Future[Result] = call(application.injector.instanceOf[EventController].getEvent(messageIdBL.toString), fakeRequestEvents)
        println(s"getBLResult=${contentAsJson(getBL)}")
        status(getBL) must equalTo(OK)
        contentType(getBL) must beSome("application/json")
        Await.result(getBL, 5.seconds)
      }
    }
  }

}
