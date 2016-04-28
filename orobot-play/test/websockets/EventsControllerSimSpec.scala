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
package websockets

import java.util.UUID

import akka.actor.ActorSystem
import akka.pattern._
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.test._
import controllers.EventController
import elastic.json.{ MqttMessageEventJson, RobotEventJson }
import elastic.views.EsView
import model.Robot
import org.specs2.mock.Mockito
import org.specs2.specification.BeforeAfterAll
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.{ FakeRequest, PlaySpecification, WithServer }
import protocol.RobotActor.Create
import protocol.RobotsSupervisor.Cmd4Robot
import utils.{ OrobotAkkaSysTestUtils, OrobotTestBaseCtx, OrobotTestUtils }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class EventsControllerSimSpec extends PlaySpecification
    with BeforeAfterAll
    with Mockito
    with MqttMessageEventJson
    with RobotEventJson {

  var system: ActorSystem = _

  val robotId = "b3d6d269-84a6-445b-b48e-5a78cc596342"
  val ts = new OrobotAkkaSysTestUtils(1882, robotId)

  override def beforeAll(): Unit = {
    ts.setup()
    println("starting system...")
    system = ts.startSystem

    val indexCreationFuture = ts.esComponent.createIndexAndMapping(ts.robotDao.asInstanceOf[EsView[Robot]])(system.dispatcher)
    Await.ready(indexCreationFuture, 3.seconds)

  }

  override def afterAll(): Unit = {
    println("stopping system...")
    Await.result(system.terminate(), 5.seconds)

    ts.clean(false)
  }

  "Events controller api" should {
    "Show events" in new TestCtx {
      override def testSetting: OrobotTestUtils = ts
      new WithServer(app = application, port = testServerPort) {

        val facade = system.actorSelection("/user/orobot/facade")
        val uuid = UUID.fromString(robotId)

        val result = (facade ? Cmd4Robot(uuid, Create(ts.customer))).map {
          case protocol.RobotActor.Initialized(robot) =>
            println(s"robot=$robot")
            assert(ts.customer == robot.customer)
            Thread.sleep(5000)
            assert(robot.id == uuid)

          case m =>
            assert(false)
        }
        Await.result(result, 6.seconds)

        Thread.sleep(40000)

        val fakeRequest = FakeRequest(GET, "").withAuthenticator[JWTAuthenticator](identity.loginInfo)

        val resultListEvents: Future[Result] = call(application.injector.instanceOf[EventController].listEvents(1, 20), fakeRequest)
        println(s"resultListEvents=${contentAsJson(resultListEvents)}")
        println(s"headers=${headers(resultListEvents)}")
        status(resultListEvents) must equalTo(OK)
        contentType(resultListEvents) must beSome("application/json")
        header("X-Total-Count", resultListEvents) must beSome("4")
        Await.result(resultListEvents, 5.seconds)

        val resultListEventsRobot: Future[Result] = call(application.injector.instanceOf[EventController].getRobotEvents(robotId, 1, 20), fakeRequest)
        println(s"resultListEventsRobot=${contentAsJson(resultListEventsRobot)}")
        println(s"headers=${headers(resultListEventsRobot)}")
        status(resultListEventsRobot) must equalTo(OK)
        contentType(resultListEventsRobot) must beSome("application/json")
        header("X-Total-Count", resultListEventsRobot) must beSome("4")
        Await.result(resultListEventsRobot, 5.seconds)

      }
    }
  }

  trait TestCtx extends OrobotTestBaseCtx {

    def testSetting: OrobotTestUtils

    /**
     * The application.
     */
    override lazy val application = new GuiceApplicationBuilder()
      .overrides(new FakeModule)
      .configure("akka.actor.serialize-creators" -> "off")
      .configure("mosquitto.broker.url" -> s"tcp://localhost:${testSetting.mqttPort}")
      .configure("customers.import.onstart" -> false)
      .configure("elastic.local.path.home" -> testSetting.esDataDirStr)
      .configure("elastic.local.path.data" -> s"${testSetting.esDataDirStr}/data")
      .configure("akka.persistence.journal.plugin" -> "inmemory-journal")
      .configure("akka.persistence.snapshot-store.plugin" -> "inmemory-snapshot-store")
      .build()
  }
}
