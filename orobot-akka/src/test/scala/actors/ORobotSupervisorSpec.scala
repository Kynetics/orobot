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

import java.util.concurrent.{ CountDownLatch, TimeUnit }

import actors.ORobotSupervisor.Env
import akka.actor.{ Actor, ActorSystem, Props }
import org.scalatest.WordSpec

class ORobotSupervisorSpec extends WordSpec {
  import ORobotSupervisorSpec._

  "ORobotSupervisor actor" when {
    "started" should {
      "create all child actors" in {
        val system = ActorSystem("ORobotSupervisorSpec-Test")
        system.actorOf(ORobotSupervisor.props(DummyEnv))
        assert(Result.countDownLatch.await(3, TimeUnit.SECONDS))
      }
    }
  }
}

object ORobotSupervisorSpec {

  object Result {
    val countDownLatch = new CountDownLatch(3)
  }

  class DummyActor extends Actor {
    val receive: Receive = PartialFunction.empty[Any, Unit]
    Result.countDownLatch.countDown()
  }

  object DummyEnv extends Env with Serializable {
    override def mqttConnectorSupervisorProps: Props = Props[DummyActor]
    override def robotRegistrySupervisorProps: Props = Props[DummyActor]
    override def robotsSupervisorProps: Props = Props[DummyActor]
    override def facadeProps: Props = Props[DummyActor]
  }

}

