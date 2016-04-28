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

import java.io.File
import java.util.UUID

import akka.actor.ActorSystem
import akka.pattern._
import akka.util.Timeout
import elastic.views.EsView
import model.Robot
import org.apache.commons.io.FileUtils
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import protocol.RobotActor.Create
import protocol.RobotsSupervisor.Cmd4Robot
import utils.OrobotAkkaSysTestUtils

import scala.concurrent.Await
import scala.concurrent.duration._

class WebSocketSimSpec extends FeatureSpec
    with BeforeAndAfterAll
    with GivenWhenThen {

  var system: ActorSystem = _

  val robotId = "c3d6d269-84a6-445b-b48e-5a78cc596342"
  val ts = new OrobotAkkaSysTestUtils(1888, robotId)

  override def beforeAll(): Unit = {
    ts.setup()
    println("starting system...")
    system = ts.startSystem

    val indexCreationFuture = ts.esComponent.createIndexAndMapping(ts.robotDao.asInstanceOf[EsView[Robot]])(system.dispatcher)
    Await.ready(indexCreationFuture, 3.seconds)

    FileUtils.forceMkdir(new File(ts.mapsDirStr))
  }

  override def afterAll(): Unit = {
    println("stopping system...")
    Await.result(system.terminate(), 5.seconds)

    ts.clean(false)
  }

  info(
    """Robot simulation with web sockect
      |ecc...
    """.stripMargin)

  feature("Robot web socket comunication") {
    scenario("A robot connect and send message") {

      implicit val ec = system.dispatcher
      implicit val timeout = Timeout(5.seconds)
      val facade = system.actorSelection("/user/orobot/facade")
      val uuid = UUID.fromString(ts.robotId)

      val result = (facade ? Cmd4Robot(uuid, Create(ts.customer))).map {
        case protocol.RobotActor.Initialized(robot) =>
          Then("the system respond with an Initialized response with the created robot")
          assertResult(ts.customer)(robot.customer)
          Thread.sleep(5000)
          assertResult(Some(robot))(Await.result(ts.robotDao.get(robot.id), 250.millis))

        case m =>
          assert(false)
      }
      Await.result(result, 10.seconds)

      Thread.sleep(16000)
    }
  }
}

