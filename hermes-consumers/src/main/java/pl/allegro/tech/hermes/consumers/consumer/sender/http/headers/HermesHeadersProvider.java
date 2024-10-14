package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers;

import static java.lang.String.valueOf;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.RETRY_COUNT;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.SCHEMA_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.SCHEMA_VERSION;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.SUBSCRIPTION_NAME;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TOPIC_NAME;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpRequestData;

public final class HermesHeadersProvider implements HttpHeadersProvider {

  private final Collection<HttpHeadersProvider> headersProvider;

  public HermesHeadersProvider(Collection<HttpHeadersProvider> headersProvider) {
    this.headersProvider = headersProvider;
  }

  @Override
  public HttpRequestHeaders getHeaders(Message message, HttpRequestData requestData) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

    headersProvider.forEach(
        provider -> builder.putAll(provider.getHeaders(message, requestData).asMap()));

    builder.put(MESSAGE_ID.getName(), message.getId());
    builder.put(RETRY_COUNT.getName(), Integer.toString(message.getRetryCounter()));

    if (message.hasSubscriptionIdentityHeaders()) {
      builder.put(TOPIC_NAME.getName(), message.getTopic());
      builder.put(SUBSCRIPTION_NAME.getName(), message.getSubscription());
    }

    message
        .getSchema()
        .ifPresent(
            schema -> builder.put(SCHEMA_VERSION.getName(), valueOf(schema.getVersion().value())));
    message
        .getSchema()
        .ifPresent(schema -> builder.put(SCHEMA_ID.getName(), valueOf(schema.getId().value())));
    message
        .getAdditionalHeaders()
        .forEach(header -> builder.put(header.getName(), header.getValue()));

    return new HttpRequestHeaders(builder.build());
  }
}
