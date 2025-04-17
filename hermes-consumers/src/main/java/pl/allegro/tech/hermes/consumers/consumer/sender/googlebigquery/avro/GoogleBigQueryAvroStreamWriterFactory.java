package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.grpc.ChannelPoolSettings;
import com.google.auth.Credentials;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteClient;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteSettings;
import com.google.cloud.bigquery.storage.v1.SchemaAwareStreamWriter;
import com.google.cloud.bigquery.storage.v1.ToProtoConverter;
import com.google.protobuf.Descriptors;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.threeten.bp.Duration;
import pl.allegro.tech.hermes.consumers.consumer.bigquery.GoogleBigQueryStreamWriterFactory;

import java.io.IOException;
import java.util.concurrent.Executors;

@Component
public class GoogleBigQueryAvroStreamWriterFactory implements GoogleBigQueryStreamWriterFactory<SchemaAwareStreamWriter<GenericRecord>> {

    private static final Logger logger = LoggerFactory.getLogger(GoogleBigQueryAvroStreamWriterFactory.class);

    private final Credentials credentials;
    private final BigQueryWriteClient writeClient;
    private final ToProtoConverter<GenericRecord> avroToProtoConverter;

    public GoogleBigQueryAvroStreamWriterFactory(CredentialsProvider credentialsProvider,
                                                 BigQueryWriteSettings writeSettings,
                                                 ToProtoConverter<GenericRecord> avroToProtoConverter) throws IOException {
        this.credentials = credentialsProvider.getCredentials();
        this.writeClient = BigQueryWriteClient.create(writeSettings);
        this.avroToProtoConverter = avroToProtoConverter;
    }

    public SchemaAwareStreamWriter<GenericRecord> getWriterForStream(String streamName) {
        try {
            return SchemaAwareStreamWriter.newBuilder(streamName, writeClient, avroToProtoConverter)
                    .setEnableConnectionPool(true)
                    .setExecutorProvider(FixedExecutorProvider.create(Executors.newScheduledThreadPool(100)))
                    .setFlowControlSettings(FlowControlSettings.newBuilder().build())
                    .setChannelProvider(BigQueryWriteSettings.defaultGrpcTransportProviderBuilder()
                            .setCredentials(credentials)
                            .setKeepAliveTime(Duration.ofMinutes(1))
                            .setKeepAliveWithoutCalls(true)
                            .setChannelPoolSettings(ChannelPoolSettings.staticallySized(2))
                            .build()
                    ).build();
        } catch (Descriptors.DescriptorValidationException | IOException | InterruptedException e) {
            logger.warn("Cannot create SchemaAwareStreamWriter for stream {}", streamName, e);
            throw new RuntimeException(e);
        }
    }
}
