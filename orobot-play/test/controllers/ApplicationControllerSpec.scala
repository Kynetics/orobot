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

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.test._
import org.specs2.mock.Mockito
import org.specs2.specification.BeforeAfterAll
import play.api.test.{ FakeRequest, PlaySpecification, WithApplication }
import utils.OrobotTestUtils

/**
 * Test case for the [[controllers.ApplicationController]] class.
 */
class ApplicationControllerSpec extends PlaySpecification with Mockito with BeforeAfterAll {

  val ts = new OrobotTestUtils(1884)

  override def beforeAll(): Unit = {
    ts.setup()
  }

  override def afterAll(): Unit = {
    ts.clean(false)
  }

  "The `index` action" should {

    "return status 401 if no authenticator was found" in new ControllersTestCtx {
      override def testSetting: OrobotTestUtils = ts
      new WithApplication(application) {

        val Some(result) = route(FakeRequest(routes.ApplicationController.user())
          .withAuthenticator[JWTAuthenticator](LoginInfo("invalid", "invalid"))
        )
        status(result) must equalTo(UNAUTHORIZED)
      }
    }

    "return 200 if user is authorized" in new ControllersTestCtx {
      override def testSetting: OrobotTestUtils = ts
      new WithApplication(application) {
        val Some(result) = route(FakeRequest(routes.ApplicationController.view("home"))
          .withAuthenticator[JWTAuthenticator](identity.loginInfo)
        )

        status(result) must beEqualTo(OK)
      }
    }
  }

  /*trait TestCtx extends OrobotTestBaseCtx {

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
  }*/
}
