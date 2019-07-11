package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers;

import com.google.common.collect.ImmutableMap;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import static java.lang.String.valueOf;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.*;

final class HermesHeadersProvider implements HttpRequestHeadersProvider {

    final static HermesHeadersProvider INSTANCE = new HermesHeadersProvider();

    private HermesHeadersProvider() {}

    @Override
    public HttpRequestHeaders getHeaders(Message message) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        builder.put(MESSAGE_ID.getName(), message.getId());
        builder.put(RETRY_COUNT.getName(), Integer.toString(message.getRetryCounter()));

        if (message.hasSubscriptionIdentityHeaders()) {
            builder.put(TOPIC_NAME.getName(), message.getTopic());
            builder.put(SUBSCRIPTION_NAME.getName(), message.getSubscription());
        }

        message.getSchema().ifPresent(schema -> builder.put(SCHEMA_VERSION.getName(), valueOf(schema.getVersion().value())));

        return new HttpRequestHeaders(builder.build());
    }

}
