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

import akka.actor.{ Actor, ActorLogging, ActorPath, ActorSelection, Props, Stash }
import akka.event.LoggingReceive
import akka.persistence.{ AbstractPersistentActor, AtLeastOnceDelivery }

object ALODProxyActor {

  sealed trait Event
  case class MsgSent(message: Any) extends Event
  case class MsgConfirmed(deliveryId: Long) extends Event

  case class Message(deliveryId: Long, msg: Any)

  def props(targetPath: ActorSelection, persistenceId: String): Props = Props(classOf[ALODProxyActor], targetPath, persistenceId)

}

class ALODProxyActor(targetPath: ActorSelection, val persistenceId: String)
    extends AbstractPersistentActor
    with AtLeastOnceDelivery
    with ActorLogging
    with Stash {
  import ALODProxyActor._

  val idle = LoggingReceive {
    case m =>
      persist(new MsgSent(m))(updateState)
  }

  val pending = LoggingReceive {
    case Message(deliveryId, m) =>
      persist(new MsgConfirmed(deliveryId)) { evt =>
        updateState(evt)
        context.parent forward m
      }

    case m => stash()
  }

  private def updateState(evt: Event): Unit = evt match {

    case MsgSent(m) =>
      deliver(targetPath)(Message(_, m))
      context become pending

    case MsgConfirmed(deliveryId) =>
      confirmDelivery(deliveryId)
      unstashAll()
      context become idle

  }

  override def receiveRecover: Receive = {
    case evt: Event => updateState(evt)
  }

  override val receiveCommand: Receive = Actor.emptyBehavior

  context become idle

}
