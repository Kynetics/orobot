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

import play.api.inject.guice.GuiceApplicationBuilder
import utils.{ OrobotTestBaseCtx, OrobotTestUtils }

trait ControllersTestCtx extends OrobotTestBaseCtx {

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
    .configure("robot.locationMap.folder" -> testSetting.mapsDirStr)
    .build()
}
