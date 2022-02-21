package pl.allegro.tech.hermes.consumers.consumer.sender.pubsub;

import com.google.common.collect.ImmutableMap;
import com.google.pubsub.v1.PubsubMessage;
import org.apache.commons.lang3.tuple.Pair;
import pl.allegro.tech.hermes.api.Header;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PubSubMetadataAppender implements MetadataAppender<PubsubMessage> {

    private final ConfigFactory configFactory;

    public PubSubMetadataAppender(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }

    @Override
    public PubsubMessage append(PubsubMessage target, Message message) {

        final Map<String, String> additionalHeaders = message.getAdditionalHeaders().stream().collect(
                Collectors.toMap(Header::getName, Header::getValue));

        return PubsubMessage.newBuilder(target)
                .putAllAttributes(additionalHeaders)
                .putAllAttributes(message.getExternalMetadata())
                .putAllAttributes(createMessageAttributes(message))
                .build();
    }

    private Map<String, String> createMessageAttributes(Message message) {
        Optional<Pair<String, String>> schemaIdAndVersion = message.getSchema().map(s ->
                Pair.of(String.valueOf(s.getId().value()), String.valueOf(s.getVersion().value())));

        final Map<String, String> headers = new HashMap<>(ImmutableMap.of(
                configFactory.getStringProperty(Configs.PUBSUB_HEADER_NAME_TOPIC_NAME), message.getTopic(),
                configFactory.getStringProperty(Configs.PUBSUB_HEADER_NAME_MESSAGE_ID), message.getId(),
                configFactory.getStringProperty(Configs.PUBSUB_HEADER_NAME_TIMESTAMP), String.valueOf(
                        message.getPublishingTimestamp())));

        schemaIdAndVersion.ifPresent(sv -> {
            headers.put(configFactory.getStringProperty(Configs.PUBSUB_HEADER_NAME_SCHEMA_ID), sv.getLeft());
            headers.put(configFactory.getStringProperty(Configs.PUBSUB_HEADER_NAME_SCHEMA_VERSION), sv.getRight());
        });

        return headers;
    }
}
