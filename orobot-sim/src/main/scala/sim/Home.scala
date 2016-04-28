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

import scalafx.scene.shape.{ Path, Rectangle, SVGPath, Shape }

object Home {

  def apply(svg: String, margin: Double): Home = {
    val m = margin
    val wallSvg = new SVGPath {
      content = svg
      translateX = m
      translateY = m
    }
    val bounds = wallSvg.boundsInLocal.get
    val frame = new Rectangle {
      width = bounds.getWidth + 2 * m
      height = bounds.getHeight + 2 * m
    }
    val homeShape = Shape.subtract(frame, wallSvg)
    new Home(new Path(homeShape.asInstanceOf[jfx.Path]))
  }
}

case class Home private (shape: Shape) extends ShapeProvider {

  def homeImageAsTxt = imageToTxt(shape.snapshot(null, null))
}
