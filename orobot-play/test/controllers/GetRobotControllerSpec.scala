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
import org.apache.commons.io.FileUtils
import org.specs2.mock.Mockito
import org.specs2.specification.BeforeAfterAll
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import utils.{ OrobotTestBaseCtx, OrobotTestUtils }

import scala.concurrent.Future

class GetRobotControllerSpec extends PlaySpecification
    with BeforeAfterAll
    with Mockito
    with CustomerJson
    with AddressJson {

  val ts = new OrobotTestUtils(1886)

  override def beforeAll(): Unit = {
    ts.setup()
  }

  override def afterAll(): Unit = {
    ts.clean(false)
  }

  def getController(application: Application) = application.injector.instanceOf[RobotController]

  "RobotController.get" should {

    "get robot successfully" in new ControllersTestCtx {
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

        val toGet = contentAsJson(result).\("id").as[String]
        val fakeRequestForGetRobot =
          FakeRequest(GET, "")
            .withAuthenticator[JWTAuthenticator](identity.loginInfo)
        val resultGet: Future[Result] = call(getController(app).getRobot(toGet), fakeRequestForGetRobot)
        status(resultGet) must equalTo(OK)
        contentType(resultGet) must beSome("application/json")
        charset(resultGet) must beSome("utf-8")

        val resultGetJson = contentAsJson(resultGet)
        println(s"resultGet=$resultGetJson")
        resultGetJson.\("state").get.\("uuid").as[String] must_=== toGet

        val notPresent = UUID.randomUUID()
        val resultNotFound: Future[Result] = call(getController(app).getRobot(notPresent.toString), fakeRequestForGetRobot)
        println(s"resultNotFound=${contentAsJson(resultNotFound)}")
        status(resultNotFound) must equalTo(NOT_FOUND)

      }
    }
  }
}
