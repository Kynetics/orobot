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
import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }
import java.lang.Math._
import java.util.Base64
import java.util.zip.{ GZIPInputStream, GZIPOutputStream }
import javax.imageio.ImageIO

import com.sun.javafx.tk.Toolkit

import scalafx.embed.swing.SwingFXUtils
import scalafx.geometry.Point2D
import scalafx.scene.image.WritableImage
import scalafx.scene.shape._

package object sim {

  trait ShapeProvider {
    def shape: Shape
  }

  val tk = Toolkit.getToolkit

  val _2PI = 2 * PI

  val radDegRatio = PI / 180.0

  val v_x = new Point2D(1.0, 0.0)

  def rad(deg: Double) = deg * radDegRatio

  def deg(rad: Double) = rad / radDegRatio

  def angle(rad: Double) =
    if (rad < -PI) _2PI + rad
    else if (rad > PI) rad - _2PI
    else rad

  def angle(v: Point2D) = rad(v.angle(v_x)) * signum(v.getY)

  def txtToImage(txt: String) = SwingFXUtils.toFXImage(ImageIO.read(new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder.decode(txt)))), null)

  def imageToTxt(image: WritableImage) = {
    val baos = new ByteArrayOutputStream
    ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", new GZIPOutputStream(baos, true))
    Base64.getEncoder.encodeToString(baos.toByteArray)
  }

}
