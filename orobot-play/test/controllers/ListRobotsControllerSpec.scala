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
import elastic.json.{ AddressJson, CustomerJson }
import model.{ Address, Customer }
import org.apache.commons.io.FileUtils
import org.specs2.mock.Mockito
import org.specs2.specification.BeforeAfterAll
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import utils.{ CSVData, OrobotTestBaseCtx, OrobotTestUtils }

import scala.concurrent.Future

class ListRobotsControllerSpec extends PlaySpecification
    with BeforeAfterAll
    with Mockito
    with CustomerJson
    with AddressJson {

  val ts = new OrobotTestUtils(1887)

  override def beforeAll(): Unit = {
    ts.setup()
  }

  override def afterAll(): Unit = {
    ts.clean(false)
  }

  def getController(application: Application) = application.injector.instanceOf[RobotController]

  "RobotController.list" should {

    "lists robots successfully" in new ControllersTestCtx {
      override def testSetting: OrobotTestUtils = ts
      new WithApplication(application) {

        val testData = new CSVData("test_customers.csv")
        val testCustomers = testData map {
          d => Json.toJson(Customer(d(0), d(1), d(2), d(3), Address(d(4), d(5), d(6), d(7)), d(8)))
        }

        Thread.sleep(3000)

        val testRobots = testCustomers.map(
          c =>
            call(getController(app).createRobot, FakeRequest(POST, "")
              .withAuthenticator[JWTAuthenticator](identity.loginInfo)
              .withJsonBody(c))
        )
        println(s"Created robost $testRobots")
        testRobots.size mustEqual (testData.size)

        Thread.sleep(3000)
        val fakeRequest =
          FakeRequest(GET, "")
            .withAuthenticator[JWTAuthenticator](identity.loginInfo)
        val result: Future[Result] = call(getController(app).listRobots(1, 20, None), fakeRequest)
        println(s"result=${contentAsJson(result)}")
        println(s"headers=${headers(result)}")
        status(result) must equalTo(OK)
        contentType(result) must beSome("application/json")
        header("X-Total-Count", result) must beSome(testData.size.toString)

        Thread.sleep(3000)
        val result2: Future[Result] = call(getController(app).listRobots(2, 20, None), fakeRequest)
        println(s"result2=${contentAsJson(result2)}")
        println(s"headers2=${headers(result)}")
        status(result2) must equalTo(OK)
        contentType(result2) must beSome("application/json")
        header("X-Total-Count", result) must beSome(testData.size.toString)
      }
    }
  }
}
