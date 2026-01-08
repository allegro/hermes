package pl.allegro.tech.hermes.consumers.consumer.result.offline;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import org.json.JSONObject;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StoreOfflineTranslator {

  public static PubsubMessage translate(
      Message message, Subscription subscription, MessageSendingResult result) {
    JSONObject jsonObject = toJsonObject(message, subscription, result);

    PubsubMessage pubsubMessage = toPubSubMessage(jsonObject);
    return  pubsubMessage;
  }

  private static JSONObject toJsonObject(
      Message message, Subscription subscription, MessageSendingResult result) {

    JSONObject jsonObject =
        new JSONObject()
            .put("topic", subscription.getTopicName().toString())
            .put("subscription", subscription.getName())
            .put("message_id", message.getId())
            .put("timestamp", message.getPublishingTimestamp())
            .put("reading_timestamp", message.getReadingTimestamp())
            .put("status_code", result.getStatusCode())
            .put("error", result.getRootCause());
    return jsonObject;
  }

  private static PubsubMessage toPubSubMessage(JSONObject jsonObject){
    PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
            .setData(ByteString.copyFromUtf8(jsonObject.toString()))
            .build();
    return pubsubMessage;
  }
}
