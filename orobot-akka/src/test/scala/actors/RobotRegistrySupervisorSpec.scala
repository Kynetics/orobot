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

import java.rmi.activation.ActivationGroup_Stub
import java.util.UUID
import java.util.concurrent.{ CountDownLatch, TimeUnit }

import actors.RobotRegistryActor.{ DeregisterRobot, RegisterRobot }
import actors.RobotRegistrySupervisor.Env
import akka.actor.{ Actor, ActorSystem, Props }
import model.Customer
import org.scalatest.WordSpec

class RobotRegistrySupervisorSpec extends WordSpec {
  import RobotRegistrySupervisorSpec._

  "RobotRegistrySupervisor actor" when {
    "started" should {
      val system = ActorSystem("RobotRegistrySupervisorSpec-Test")
      val rrs = system.actorOf(RobotRegistrySupervisor.props(DummyEnv))

      "create a registry child actor" in {
        assert(Result.countDownLatch1.await(1, TimeUnit.SECONDS))
      }

      "foward any RobotRegistryActor.Received msg to registry" in {
        rrs ! RegisterRobot(null, null)
        rrs ! DeregisterRobot(null)
        assert(Result.countDownLatch2.await(1, TimeUnit.SECONDS))
      }
    }
  }
}

object RobotRegistrySupervisorSpec {
  object Result {
    val countDownLatch1 = new CountDownLatch(1)
    val countDownLatch2 = new CountDownLatch(2)
  }

  class DummyActor extends Actor {
    val receive: Receive = {
      case c: RobotRegistryActor.Received => Result.countDownLatch2.countDown()
    }
    Result.countDownLatch1.countDown()
  }

  object DummyEnv extends Env with Serializable {
    def robotRegistryActorProps: Props = Props[DummyActor]
  }
}

