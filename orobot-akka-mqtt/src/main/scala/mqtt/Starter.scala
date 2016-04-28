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
package mqtt

import akka.actor.ActorSystem
import com.typesafe.config.{ ConfigFactory, ConfigValueFactory }
import mqtt.actors.DefaultEnv

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

object Starter extends DefaultEnv {

  override def cfg = ConfigFactory.load()

  def main(args: Array[String]) {
    val cfg = ConfigFactory.load()
      .withValue("akka.actor.serialize-creators", ConfigValueFactory.fromAnyRef("off"))
    val system = ActorSystem("MqttConnector", cfg)
    mqtt.actors.initSystem(system, Starter)
    import system.dispatcher
    Source.stdin.getLines().foreach {
      case "q" => Await.ready(system.terminate(), 5.seconds).onFailure { case e: Exception => println(e) }
      case s => Console.println(s"${Console.RED} command $s not supported${Console.BLACK}")
    }
  }
}
