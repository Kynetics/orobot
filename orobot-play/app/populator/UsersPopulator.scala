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
package populator

import java.io.File
import java.util.UUID

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.impl.util.BCryptPasswordHasher
import forms.SignUpForm
import models.User
import models.daos.UserDAOImpl
import play.api.{ Application, Logger }

import scala.io.BufferedSource

case class UsersPopulator(app: Application) {

  def populateUsers(filePath: String, backupFile: Boolean = true) = {
    import akka.util.Timeout

    import scala.concurrent.duration._
    implicit val timeout: Timeout = 5.seconds
    val signUps: Set[SignUpForm.Data] = readSignupsFromFile(filePath, backupFile)
    Logger.info(s"Starting import of ${signUps.size} accounts from $filePath")
    signUps.foreach { data =>
      println(data)

      val authInfoRepository = app.injector.instanceOf[AuthInfoRepository]

      val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
      println(loginInfo)
      val passwordHasher = new BCryptPasswordHasher
      val authInfo = passwordHasher.hash(data.password)
      val user = User(
        userID = UUID.randomUUID(),
        loginInfo = loginInfo,
        firstName = Some(data.firstName),
        lastName = Some(data.lastName),
        fullName = Some(data.firstName + " " + data.lastName),
        email = Some(data.email)
      )
      println(user)
      val userDao = new UserDAOImpl
      userDao save user
      println(UserDAOImpl.users)
      authInfoRepository.add(loginInfo, authInfo)
    }
  }

  private def readSignupsFromFile(path: String, backupFile: Boolean): Set[SignUpForm.Data] = {
    var signUps: Set[SignUpForm.Data] = Set.empty
    import scala.io.Source
    try {
      val bufferedSource: BufferedSource = Source.fromFile(path)
      var counter = 0
      for (line <- bufferedSource.getLines.drop(1)) {
        val cols = line.split(";").map(_.trim)
        counter = counter + 1
        // cols format: 0 username; 1 password
        val signUp = SignUpForm.Data(
          firstName = cols(0),
          lastName = cols(1),
          email = cols(2),
          password = cols(3)
        )
        signUps = signUps + signUp
      }
      bufferedSource.close
      if (backupFile) {
        new File(path).renameTo(new File(path + ".imported"))
      }
    } catch {
      case ex: Exception => Logger.warn(s"File $path not found.")
    }
    signUps
  }

}

