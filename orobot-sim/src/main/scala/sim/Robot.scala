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

import scalafx.scene.shape.{ Circle, Line, Shape }
import scalafx.Includes._
import scalafx.geometry.Point2D
import Math._

import scalafx.scene.paint.Color

object Robot {

  def apply(scale: Double): Robot = {
    val c = new Circle() {
      centerX = 0
      centerY = 0
      radius = 0.3 / scale
    }
    val e1 = new Line {
      startX = 0.15 / scale
      startY = 0.10 / scale
      endX = startX.get
      endY = startY.get + 0.05 / scale
    }
    val e2 = new Line {
      startX = e1.startX.get
      startY = -e1.startY.get
      endX = startX.get
      endY = -e1.endY.get
    }
    new Robot(Shape.subtract(c, Shape.union(e1, e2)), c.radius.get, scale)
  }

  private val turnThreshold = PI / 50
  private val moveThreshold = PI / 5
}

class Robot private (val shape: Shape, radius: Double, scale: Double) extends ShapeProvider {
  import Robot._

  var omega: Double = 0.0

  var velocity = 0.0

  private var _connected: Boolean = true

  def position = new Point2D(shape.translateX.get, shape.translateY.get)

  def position_=(p: Point2D): Unit = {
    shape.translateX = p.x
    shape.translateY = p.y
  }

  def heading: Double = angle(rad(shape.rotate.get))

  def heading_=(ang: Double) = shape.rotate = deg(angle(ang))

  def touch(point: Point2D) = position.distance(point) < radius

  def moveTo(target: Point2D, dt: Double): Unit = {
    val p = position
    val h = heading
    val walk = target.subtract(p)
    val direction = angle(walk)
    val turn = angle(direction - h)
    val turnSize = abs(turn)
    omega = if (turnSize > turnThreshold) signum(turn) else 0.0
    velocity = if (turnSize < moveThreshold) 1.0 / scale else 0.0
    val theta = h + omega * dt
    val ds = velocity * dt
    val dx = ds * cos(theta)
    val dy = ds * sin(theta)
    heading = theta
    position = p.add(dx, dy)
  }

  def connected_=(bool: Boolean): Unit = bool match {
    case true =>
      shape.fill = Color.White
      _connected = true
    case false =>
      shape.fill = Color.Orange
      _connected = false
  }

  def connected = _connected

}
