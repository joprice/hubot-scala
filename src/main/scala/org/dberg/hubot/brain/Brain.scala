package org.dberg.hubot.brain

import org.dberg.hubot.utils.Helpers._

trait BrainComponent {

  lazy val brainService: BrainBackendBase = {
    getConfString("hubot.brain", "mapdb") match {
      case "mapdb" => MapdbBackend
      case "dynamodb" =>
        val tableName = "hubot-test"
        new DynamoDBBackend(tableName)
      case _ => MapdbBackend
    }
  }

}

