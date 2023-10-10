package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.EndpointAddressResolverMetadata;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.BatchHttpHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpRequestHeaders;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolutionException;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static pl.allegro.tech.hermes.api.AvroMediaType.AVRO_BINARY;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.BATCH_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.RETRY_COUNT;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.SUBSCRIPTION_NAME;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TOPIC_NAME;

public class JettyMessageBatchSender implements MessageBatchSender {

    private static final Logger logger = LoggerFactory.getLogger(JettyMessageBatchSender.class);

    private final BatchHttpRequestFactory requestFactory;
    private final EndpointAddressResolver resolver;
    private final SendingResultHandlers resultHandlers;
    private final BatchHttpHeadersProvider headersProvider;

    public JettyMessageBatchSender(BatchHttpRequestFactory requestFactory,
                                   EndpointAddressResolver resolver,
                                   SendingResultHandlers resultHandlers,
                                   BatchHttpHeadersProvider headersProvider) {
        this.requestFactory = requestFactory;
        this.resolver = resolver;
        this.resultHandlers = resultHandlers;
        this.headersProvider = headersProvider;
    }

    @Override
    public MessageSendingResult send(MessageBatch batch,
                                     EndpointAddress address,
                                     EndpointAddressResolverMetadata metadata,
                                     int requestTimeout) {
        try {
            HttpRequestHeaders headers = headersProvider.getHeaders(address);
            return send(batch, resolver.resolve(address, batch, metadata), requestTimeout, headers);
        } catch (EndpointAddressResolutionException e) {
            return MessageSendingResult.failedResult(e);
        }
    }

    private MessageSendingResult send(MessageBatch batch, URI address, int requestTimeout, HttpRequestHeaders baseHeaders) {
        HttpRequestHeaders headers = buildHeaders(batch, baseHeaders);
        Request request = requestFactory.buildRequest(batch, address, headers, requestTimeout);
        try {
            ContentResponse response = request.send();
            return resultHandlers.handleSendingResultForBatch(response);
        } catch (TimeoutException | ExecutionException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                logger.info("Restoring interrupted status", e);
            }
            throw new HttpBatchSenderException("Failed to send message batch", e);
        }
    }

    private HttpRequestHeaders buildHeaders(MessageBatch batch, HttpRequestHeaders baseHeaders) {
        Map<String, String> headers = new HashMap<>(baseHeaders.asMap());

        ContentType contentType = getMediaType(batch.getContentType());
        headers.put(BATCH_ID.getName(), batch.getId());
        headers.put(HTTP.CONTENT_TYPE, contentType.getMimeType());
        headers.put(RETRY_COUNT.getName(), Integer.toString(batch.getRetryCounter()));

        if (batch.hasSubscriptionIdentityHeaders()) {
            headers.put(TOPIC_NAME.getName(), batch.getTopic());
            headers.put(SUBSCRIPTION_NAME.getName(), batch.getSubscription().getName());
        }

        batch.getAdditionalHeaders().forEach(header -> headers.put(header.getName(), header.getValue()));
        return new HttpRequestHeaders(headers);
    }

    private ContentType getMediaType(pl.allegro.tech.hermes.api.ContentType contentType) {
        return AVRO.equals(contentType) ? ContentType.create(AVRO_BINARY) : ContentType.APPLICATION_JSON;
    }
}
