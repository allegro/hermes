package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.common.collect.ImmutableMap;
import com.google.pubsub.v1.PubsubMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

public class GooglePubSubMetadataAppender implements MetadataAppender<PubsubMessage> {

  public static final String HEADER_NAME_TOPIC_NAME = "tn";
  public static final String HEADER_NAME_MESSAGE_ID = "id";
  public static final String HEADER_NAME_SUBSCRIPTION_NAME = "sn";
  public static final String HEADER_NAME_TIMESTAMP = "ts";
  public static final String HEADER_NAME_SCHEMA_ID = "sid";
  public static final String HEADER_NAME_SCHEMA_VERSION = "sv";

  @Override
  public PubsubMessage append(PubsubMessage target, Message message) {
    return PubsubMessage.newBuilder(target)
        .putAllAttributes(createMessageAttributes(message))
        .build();
  }

  protected Map<String, String> createMessageAttributes(Message message) {
    Optional<Pair<String, String>> schemaIdAndVersion =
        message
            .getSchema()
            .map(
                s ->
                    Pair.of(
                        String.valueOf(s.getId().value()), String.valueOf(s.getVersion().value())));

    final Map<String, String> headers =
        new HashMap<>(
            ImmutableMap.of(
                HEADER_NAME_TOPIC_NAME, message.getTopic(),
                HEADER_NAME_MESSAGE_ID, message.getId(),
                HEADER_NAME_TIMESTAMP, String.valueOf(message.getPublishingTimestamp())));

    if (message.hasSubscriptionIdentityHeaders()) {
      headers.put(HEADER_NAME_SUBSCRIPTION_NAME, message.getSubscription());
    }

    schemaIdAndVersion.ifPresent(
        sv -> {
          headers.put(HEADER_NAME_SCHEMA_ID, sv.getLeft());
          headers.put(HEADER_NAME_SCHEMA_VERSION, sv.getRight());
        });

    return headers;
  }
}
