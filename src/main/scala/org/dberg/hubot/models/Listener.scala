package org.dberg.hubot.models

import java.util.regex.Pattern

import org.dberg.hubot.Hubot
import org.dberg.hubot.utils.Logger
import org.dberg.hubot.models.ListenerType.ListenerValue
import org.dberg.hubot.utils.Helpers._

object ListenerType {
  sealed trait ListenerValue
  case object Respond extends ListenerValue
  case object Hear extends ListenerValue
}


abstract class Listener(matcher: String, listenerType: ListenerValue = ListenerType.Respond)  {

  val pattern = matcher.r

  def call(message: Message): Unit = {
    if ( shouldRespond(message) ) {
      pattern.findFirstIn(message.body.removeBotString) match {
        case None => Logger.log("no match for listner " + this.getClass.getName)
        case Some(x) => runCallback(message.copy(body = message.body.removeBotString))
      }
    }
  }

  def shouldRespond(message: Message): Boolean = {
    listenerType == ListenerType.Hear || (listenerType ==  ListenerType.Respond && message.body.addressedToHubot)
  }

  def runCallback(message: Message): Unit

  def helpString: Option[String]
}


//-------------------------------------
// SOME TEST LISTENERS FOR NOW
//-------------------------------------
case class TestListener() extends Listener("listen1\\s+") {

  def runCallback(message: Message) = {
    val resp = "listen1 heard " + message.body
    Logger.log("Running callback for listner TestListener, sending response " + resp,"debug")
    Hubot.robot.send(Message(message.user,resp))
  }

  val helpString = Some("listen1 -> Responds to anything and repeats it ")
}

case class TestListener2() extends Listener("listen2") {

  def runCallback(message: Message) = {
    Logger.log("Running callback for listner TestListener2","debug")
    Hubot.robot.send(Message(message.user,message.body.reverse))
  }

  val helpString = Some("listen2 -> reverses anything you send it ")

}

case class HelpListener() extends Listener("^help") {

  def runCallback(message: Message) = {
    Logger.log("Running help listener","debug")
    Hubot.robot.send(Message(message.user,"Help commands \n" + Hubot.helpCommands.mkString("\n")))
  }

  val helpString = Some("help -> list all available commands ")
}
