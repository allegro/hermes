package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolutionException;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;

import java.io.IOException;
import java.net.URI;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.BATCH_ID;
import static pl.allegro.tech.hermes.consumers.consumer.sender.http.AvroMediaType.AVRO_BINARY;

public class ApacheHttpClientMessageBatchSender implements MessageBatchSender {

    private final int connectionTimeout;
    private final int socketTimeout;
    private final EndpointAddressResolver resolver;

    private CloseableHttpClient client = HttpClients.createMinimal();

    public ApacheHttpClientMessageBatchSender(int connectionTimeout, int socketTimeout, EndpointAddressResolver resolver) {
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
        this.resolver = resolver;
    }

    @Override
    public MessageSendingResult send(MessageBatch batch, EndpointAddress address, int requestTimeout) {
        try {
            return send(batch, resolver.resolve(address, batch), requestTimeout);
        } catch (EndpointAddressResolutionException e) {
            return MessageSendingResult.failedResult(e);
        }
    }

    public MessageSendingResult send(MessageBatch batch, URI address, int requestTimeout) {
        ContentType contentType = getMediaType(batch.getContentType());
        HttpPost httpPost = new HttpPost(address);
        ByteBufferEntity entity = new ByteBufferEntity(batch.getContent(), contentType);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectionTimeout)
                .setConnectionRequestTimeout(requestTimeout)
                .setSocketTimeout(socketTimeout)
                .build();

        httpPost.setConfig(requestConfig);
        httpPost.setEntity(entity);
        httpPost.addHeader(HTTP.CONN_KEEP_ALIVE, "true");
        httpPost.addHeader(BATCH_ID.getName(), batch.getId());
        httpPost.addHeader(HTTP.CONTENT_TYPE, contentType.getMimeType());
        return send(httpPost);
    }

    private MessageSendingResult send(HttpPost post) {
        try {
            return new MessageSendingResult(client.execute(post).getStatusLine().getStatusCode());
        } catch (IOException e) {
            return new MessageSendingResult(e);
        } finally {
            post.releaseConnection();
        }
    }

    public ContentType getMediaType(pl.allegro.tech.hermes.api.ContentType contentType) {
        return AVRO.equals(contentType) ? ContentType.create(AVRO_BINARY) : ContentType.APPLICATION_JSON;
    }
}
