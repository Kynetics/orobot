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

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._

object Simulation {

  sealed trait Event

  private def getSvg(config: Config, key: String) =
    config.as[String](key).stripMargin.replaceAll("""\n""", "")

  def apply(config: Config)(callback: Event => Unit = (_ => ())): Simulation = {
    val scale = config.getAs[Double]("scale").getOrElse(1.0)
    val margin = 0.20 / scale
    new Simulation(
      Home(getSvg(config, "wallPath"), margin),
      Walk(getSvg(config, "walkPath"), margin),
      Robot(scale),
      scale)
  }
}

class Simulation(
    val home: Home,
    val walk: Walk,
    val robot: Robot,
    val scale: Double) {

  robot.position = walk.point(0)
  var step = 0

  def evolve(dt: Double): Unit = {
    val target = walk.point(step)
    if (robot.touch(target)) {
      step += 1
      evolve(dt)
    } else robot.moveTo(target, dt)
  }
}
