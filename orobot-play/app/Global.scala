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
import play.api.{ Application, GlobalSettings }
import populator.UsersPopulator

object Global extends GlobalSettings {

  override def onStart(app: Application): Unit = {
    super.onStart(app)
    val c = app.configuration

    populateUsers(app)
  }

  override def onStop(app: Application): Unit = {
    super.onStop(app)
  }

  private def populateUsers(app: Application) = {
    val executeBatchImport = app.configuration.getBoolean("users.import.onstart").getOrElse(false)
    if (executeBatchImport) {
      val batchCsvPath = app.configuration.getString("users.import.file.csv").getOrElse("./users.csv")
      val renameFile = app.configuration.getBoolean("users.import.file.rename").getOrElse(false)
      UsersPopulator(app).populateUsers(batchCsvPath, renameFile)
    }
  }

}

