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
package utils

import java.util.UUID

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.{ Environment, LoginInfo }
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.test.FakeEnvironment
import models.User
import net.codingwell.scalaguice.ScalaModule
import org.specs2.specification.Scope
import play.api.inject.guice.GuiceApplicationBuilder
import scala.concurrent.ExecutionContext.Implicits.global

trait OrobotTestBaseCtx extends Scope {
  /**
   * A fake Guice module.
   */
  class FakeModule extends AbstractModule with ScalaModule {
    def configure() = {
      bind[Environment[User, JWTAuthenticator]].toInstance(env)
    }
  }

  /**
   * An identity.
   */
  val identity = User(
    userID = UUID.randomUUID(),
    loginInfo = LoginInfo("user", "user@kynetics.it"),
    firstName = None,
    lastName = None,
    fullName = None,
    email = None
  )

  /**
   * A Silhouette fake environment.
   */
  implicit val env: Environment[User, JWTAuthenticator] = new FakeEnvironment[User, JWTAuthenticator](Seq(identity.loginInfo -> identity))

  /**
   * The application.
   */
  lazy val application = new GuiceApplicationBuilder()
    .overrides(new FakeModule)
    .build()

}
