package org.dberg.hubot.brain

import com.amazonaws.services.dynamodbv2.{ AmazonDynamoDBClientBuilder, AmazonDynamoDB, AmazonDynamoDBClient }
import scodec.{ Codec => SCodec, _ }
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }
import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.ClientConfiguration
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.document.{ DynamoDB, Item }

object DynamoDBBackend {
  def dynamoDBClient(
    endpoint: Option[String],
    credentials: Option[AWSCredentialsProvider]
  ): AmazonDynamoDB = {
    val config = new ClientConfiguration()
      .withConnectionTimeout(1.second.toMillis.toInt)
      .withSocketTimeout(1.second.toMillis.toInt)
    val builder = AmazonDynamoDBClientBuilder
      .standard()
      .withClientConfiguration(config)
    val withEndpoint = endpoint.fold(builder) { endpoint =>
      builder.withEndpointConfiguration(new EndpointConfiguration(endpoint, builder.getRegion))
    }
    credentials.fold(withEndpoint) { credentials =>
      withEndpoint.withCredentials(credentials)
    }
      .build()
  }
}

class DynamoDBBackend(
    tableName: String
) extends BrainBackendBase with StrictLogging {
  import scala.collection.JavaConverters._

  private val client = DynamoDBBackend.dynamoDBClient(None, None)
  private val table = new DynamoDB(client).getTable(tableName)

  def set[A: SCodec](key: String, value: A) = {
    encode(value).flatMap { data =>
      Try(
        table.putItem(
          new Item()
            .withString("id", key)
            .withBinary("data", data)
        )
      )
    }
  }

  def get[A: SCodec](key: String): Try[A] = Try {
    table.getItem("id", key)
  }.flatMap { result =>
    if (result != null) {
      Success(result.getBinary("data"))
    } else Failure(new Exception(s"No item found for key $key"))
  }.flatMap(decode[A](_))

  def shutdown() = client.shutdown()
}
