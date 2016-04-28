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

import java.io.{ ByteArrayOutputStream, File, IOException }
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.Base64
import java.util.concurrent.{ CountDownLatch, TimeUnit, TimeoutException }
import java.util.zip.GZIPOutputStream

import model.{ Address, Customer }
import org.apache.commons.io.FileUtils
import play.api.Play

import scala.sys.process.{ Process, ProcessLogger }
import scala.util.{ Failure, Random, Success, Try }

class OrobotTestUtils(val mqttPort: Int) {

  var mosquitto: Process = _

  val rndDirName = new String(Random.alphanumeric.take(8).toArray)
  val esDataDirStr = s"es-test-$rndDirName"
  val mapsDirStr = s"maps-test-$rndDirName"

  private def startMosquitto: Try[Process] = Try {

    val cl = new CountDownLatch(1)
    val pl = ProcessLogger { line =>
      println(line)
      if (line.contains(s"Opening ipv6 listen socket on port $mqttPort."))
        cl.countDown()
    }
    import scala.sys.process._
    val path = System.getenv("PATH")
    val whichMosquitto = Process(Seq("which", "mosquitto"), None, "PATH" -> s"$path:/usr/local/sbin").lineStream_!
    val proc = s"${whichMosquitto.mkString} -p $mqttPort".run(pl)
    if (cl.await(3L, TimeUnit.SECONDS))
      proc
    else
      throw new TimeoutException()
  }

  /*def deleteTestDirs(toDelete: String) = Files.walkFileTree(Paths.get(toDelete), new SimpleFileVisitor[Path] {
    def delete(path: Path) = { Files.delete(path); FileVisitResult.CONTINUE }
    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = delete(file)
    override def postVisitDirectory(dir: Path, ioe: IOException): FileVisitResult = delete(dir)
  })*/

  private def deleteTestDirs(toDelete: String) = FileUtils.deleteDirectory(new File(toDelete))

  def setup(): Unit = {
    println("starting mosquitto...")
    startMosquitto match {
      case Failure(e) =>
        println(s"unable to start mosquitto due to: $e")

      case Success(mosquittoProc) =>
        println("mosquitto succesfully started.")
        mosquitto = mosquittoProc
    }

    Thread.sleep(1000) //wait for startup

    Files.createDirectory(Paths.get(mapsDirStr))
  }

  def clean(deleteDirs: Boolean): Unit = {
    Thread.sleep(3000)
    println("stopping mosquitto...")
    mosquitto.destroy()
    println(s"mosquitto stopped with exitCode: ${mosquitto.exitValue()}")

    if (deleteDirs) {
      println(s"remove es data $esDataDirStr ...")
      deleteTestDirs(esDataDirStr)

      println(s"remove maps data $mapsDirStr ...")
      deleteTestDirs(mapsDirStr)
    }
  }

  val customer = Customer(
    "TSTTST70A01G224G ",
    "test",
    "test",
    "test.test@domain.com",
    Address(
      "via Roma 15/1",
      "35100",
      "Padova",
      "PD"
    ),
    "3481234567"
  )

  val customerJson = """
  {
    "codiceFiscale" : "TSTSRU75A41G224L ",
    "firstName": "user",
    "lastName": "test",
    "email": "test@domain.it",
    "address": {
      "street": "prova",
      "cap": "12345",
      "city": "padova",
      "pv": "PD"
    },
    "phone": "123456789"
  }"""

  def imageToTxt(filename: String) = {
    import play.api.Play.current
    val in = Play.resourceAsStream(filename)
    val bytes = new Array[Byte](in.get.available())
    in.get.read(bytes)
    in.get.close()

    val arrOutputStream = new ByteArrayOutputStream()
    val zipOutputStream = new GZIPOutputStream(arrOutputStream)
    zipOutputStream.write(bytes)
    zipOutputStream.close()
    Base64.getEncoder.encodeToString(arrOutputStream.toByteArray)
  }
}
