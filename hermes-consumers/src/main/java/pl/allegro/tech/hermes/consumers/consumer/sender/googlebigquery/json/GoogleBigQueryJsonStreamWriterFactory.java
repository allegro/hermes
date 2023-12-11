package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json;

import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.grpc.ChannelPoolSettings;
import com.google.auth.Credentials;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteClient;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteSettings;
import com.google.cloud.bigquery.storage.v1.JsonStreamWriter;
import com.google.protobuf.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.threeten.bp.Duration;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.GoogleBigQueryStreamWriterFactory;

import java.io.IOException;
import java.util.concurrent.Executors;

@Component
public class GoogleBigQueryJsonStreamWriterFactory implements GoogleBigQueryStreamWriterFactory<JsonStreamWriter> {

    private static final Logger logger = LoggerFactory.getLogger(GoogleBigQueryJsonDataWriter.class);

    private final Credentials credentials;
    private final BigQueryWriteClient writeClient;

    public GoogleBigQueryJsonStreamWriterFactory(CredentialsProvider credentials, BigQueryWriteSettings writeSettings) throws IOException {
        this.credentials = credentials.getCredentials();
        this.writeClient = BigQueryWriteClient.create(writeSettings);
    }

    public JsonStreamWriter getWriterForStream(String stream) {
        try {
            return JsonStreamWriter.newBuilder(stream, writeClient)
                    .setEnableConnectionPool(true)
                    .setExecutorProvider(FixedExecutorProvider.create(Executors.newScheduledThreadPool(100)))
                    .setFlowControlSettings(FlowControlSettings.newBuilder().build())
                    .setChannelProvider(BigQueryWriteSettings.defaultGrpcTransportProviderBuilder()
                            .setCredentials(credentials)
                            .setKeepAliveTime(Duration.ofMinutes(1))
                            .setKeepAliveWithoutCalls(true)
                            .setChannelPoolSettings(ChannelPoolSettings.staticallySized(2))
                            .build())
                    .build();
        } catch (Descriptors.DescriptorValidationException | IOException | InterruptedException e) {
            logger.warn("Cannot create JsonStreamWriter for stream {}", stream, e);
            throw new RuntimeException(e);
        }
    }
}
