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

import javafx.scene.{ shape => jfx }

import scala.collection.JavaConverters._
import scalafx.geometry.Point2D
import scalafx.scene.shape._
import scalafx.Includes._

object Walk {

  def apply(svg: String, margin: Double): Walk = {
    val walkSvg = new SVGPath {
      content = svg
    }
    val path2D = tk.createSVGPath2D(walkSvg)
    val jfxPath = new jfx.Path(tk.convertShapeToFXPath(path2D): _*)
    val m = margin
    new Walk(new Path(jfxPath) {
      translateX = m
      translateY = m
    })
  }

}

case class Walk private (shape: Path) extends ShapeProvider {

  val stepCount = shape.elements.last match {
    case _: jfx.ClosePath => shape.elements.size - 1
    case _ => shape.elements.size
  }

  def point(step: Int): Point2D = {
    val dx = shape.translateX.get
    val dy = shape.translateY.get
    shape.elements(stepIndex(step)) match {
      case m: jfx.MoveTo => new Point2D(m.x.get + dx, m.y.get + dy)
      case m: jfx.LineTo => new Point2D(m.x.get + dx, m.y.get + dy)
    }
  }

  private def stepIndex(index: Int): Int = (stepCount + index % stepCount) % stepCount

  object stroke

}
