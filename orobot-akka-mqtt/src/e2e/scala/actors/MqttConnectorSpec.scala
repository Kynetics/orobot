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
package actors

import java.util.UUID
import java.util.concurrent.{CountDownLatch, TimeUnit, TimeoutException}

import akka.actor.ActorSystem
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import mqtt.actors.DefaultEnv
import mqtt.actors.MqttConnectorActor.Register
import org.scalatest.{BeforeAndAfterAll, FeatureSpec, GivenWhenThen}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.sys.process.{Process, ProcessLogger}
import scala.util.{Failure, Success, Try}

class MqttConnectorSpec extends FeatureSpec
  with BeforeAndAfterAll
  with GivenWhenThen {

  import MqttConnectorSpec._

  var mosquitto: Process = _
  var system: ActorSystem = _

  override def beforeAll(): Unit = {

    println("starting mosquitto...")
    startMosquittoOnPort(mqttPort) match {
      case Failure(e) =>
        println(s"unable to start mosquitto due to: $e")
        fail(e)

      case Success(mosquittoProc) =>
        println("mosquitto succesfully started.")
        mosquitto = mosquittoProc
    }

    println("startng akka system...")
    system = startAkkaSystem(mqttPort)

  }

  override def afterAll(): Unit = {
    println("stopping akka system...")
    Await.result(system.terminate(), 5.seconds)

    println("stopping mosquitto...")
    mosquitto.destroy()
    println(s"mosquitto stopped with exitCode: ${mosquitto.exitValue()}")
  }

  feature("Robot register to MCA") {
    scenario("Robot send register message") {
      When("mqtt connector receives register massage")
      val mca = system.actorSelection("/user/mqttconnectorsv/mqttconnector")
      Thread.sleep(5000)
      mca ! Register(robotId)
      Then("the robot should be registered")
      //TODO assert
    }
  }
}

object MqttConnectorSpec extends DefaultEnv {
  val mqttPort = 1884

  override def cfg = ConfigFactory.empty()
    .withValue(
      "mosquitto.broker.url",
      ConfigValueFactory.fromAnyRef(s"tcp://localhost:$mqttPort"))

  val robotId = UUID.randomUUID()

  private def startMosquittoOnPort(port: Int): Try[Process] = Try {
    val cl = new CountDownLatch(1)
    val pl = ProcessLogger { line =>
      println(line)
      if (line.contains(s"Opening ipv6 listen socket on port $port."))
        cl.countDown()
    }
    import scala.sys.process._
    val path = System.getenv("PATH")
    val whichMosquitto = Process(Seq("which", "mosquitto"), None, "PATH" -> s"$path:/usr/local/sbin").lineStream_!
    val proc = s"${whichMosquitto.mkString} -p $port".run(pl)
    if (cl.await(3L, TimeUnit.SECONDS))
      proc
    else
      throw new TimeoutException()
  }

  private def startAkkaSystem(mqttPort: Int): ActorSystem = {
    val system = ActorSystem("MqttConnectorSpec", cfg)
    mqtt.actors.initSystem(system, this)
    system
  }

}
