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

import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.test._
import org.apache.commons.io.FileUtils
import org.specs2.mock.Mockito
import org.specs2.specification.BeforeAfterAll
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import utils.OrobotTestUtils

import scala.concurrent.Future

class CreateRobotControllerSpec extends PlaySpecification
    with BeforeAfterAll
    with Mockito {

  val ts = new OrobotTestUtils(1885)

  override def beforeAll(): Unit = {
    ts.setup()
  }

  override def afterAll(): Unit = {
    ts.clean(false)
  }

  def getController(application: Application) = application.injector.instanceOf[RobotController]

  "RobotController.create" should {

    "create a new Robot successfully" in new ControllersTestCtx {
      override def testSetting: OrobotTestUtils = ts
      new WithApplication(application) {

        val fakeRequestForCreateRobot =
          FakeRequest(POST, "")
            .withAuthenticator[JWTAuthenticator](identity.loginInfo)
            .withJsonBody(Json.parse(ts.customerJson))

        Thread.sleep(3000)

        val result: Future[Result] = call(getController(app).createRobot, fakeRequestForCreateRobot)
        println(s"result=${contentAsJson(result)}")
        status(result) must equalTo(CREATED)
        contentType(result) must beSome("application/json")
        charset(result) must beSome("utf-8")
      }
    }
  }
}

