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
package sim

import java.util.concurrent.{ Executors, TimeUnit }

import com.typesafe.config.ConfigFactory

import scalafx.application.{ JFXApp, Platform }
import scalafx.scene.paint.Color
import scalafx.scene.{ Group, Scene }

trait SimApp {
  def sim: Simulation
  def myStage = new JFXApp.PrimaryStage {
    title.value = "Hello Robot"
    val home = sim.home.shape
    val walk = sim.walk.shape
    val robo = sim.robot.shape
    home.fill = Color.Black
    walk.stroke = Color.Blue
    robo.fill = Color.LightGray
    robo.stroke = Color.DarkGray
    val g = new Group(home, walk, robo)
    scene = new Scene {
      content = g
    }
  }

}

object StageDemo extends JFXApp with SimApp {
  lazy val s = Simulation(ConfigFactory.load().getConfig("sim.env"))()
  override def sim = s
  stage = myStage
  val task = new Runnable() {
    def run() = {
      Platform.runLater {
        sim.evolve(1.0 / 60)
      }
    }
  }
  Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(task, 0, 1000 / 60, TimeUnit.MILLISECONDS)

}
