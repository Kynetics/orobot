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

import akka.actor.ActorSystem
import com.typesafe.config.{ ConfigFactory, ConfigValueFactory }

import scala.concurrent.Await
import scala.io.Source
import scala.concurrent.duration._

object Starter extends env.DefaultEnvironment {

  override def cfg = ConfigFactory.load()

  def main(args: Array[String]) {
    cfg
      .withValue(
        "akka.persistence.journal.plugin",
        ConfigValueFactory.fromAnyRef("inmemory-journal"))
      .withValue(
        "akka.persistence.snapshot-store.plugin",
        ConfigValueFactory.fromAnyRef("inmemory-snapshot-store"))
      .withValue("akka.actor.serialize-creators", ConfigValueFactory.fromAnyRef("off"))
    val system = ActorSystem("ORobot", cfg)
    actors.initSystem(system, Starter)
    import system.dispatcher
    Source.stdin.getLines().foreach {
      case "q" => Await.ready(system.terminate(), 5.seconds).onFailure { case e: Exception => println(e) }
      case s => Console.println(s"${Console.RED} command $s not supported${Console.BLACK}")
    }
  }
}
