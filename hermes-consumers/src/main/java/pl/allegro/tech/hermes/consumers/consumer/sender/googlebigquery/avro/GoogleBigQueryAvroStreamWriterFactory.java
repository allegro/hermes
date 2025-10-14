package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.grpc.ChannelPoolSettings;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.Credentials;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteClient;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteSettings;
import com.google.cloud.bigquery.storage.v1.SchemaAwareStreamWriter;
import com.google.cloud.bigquery.storage.v1.ToProtoConverter;
import java.io.IOException;
import java.util.concurrent.Executors;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Duration;
import pl.allegro.tech.hermes.consumers.config.GoogleBigQueryAvroStreamWriterProperties;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.GoogleBigQueryStreamWriterFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.ThreadPoolProvider;

public class GoogleBigQueryAvroStreamWriterFactory
    implements GoogleBigQueryStreamWriterFactory<SchemaAwareStreamWriter<GenericRecord>> {

  private static final Logger logger =
      LoggerFactory.getLogger(GoogleBigQueryAvroStreamWriterFactory.class);

  private final GoogleBigQueryAvroStreamWriterProperties avroStreamWriterProperties;
  private final Credentials credentials;
  private final ThreadPoolProvider threadPoolProvider;
  private final BigQueryWriteClient writeClient;
  private final ToProtoConverter<GenericRecord> avroToProtoConverter;

  public GoogleBigQueryAvroStreamWriterFactory(
      GoogleBigQueryAvroStreamWriterProperties avroStreamWriterProperties,
      CredentialsProvider credentialsProvider,
      ThreadPoolProvider threadPoolProvider,
      BigQueryWriteSettings writeSettings,
      ToProtoConverter<GenericRecord> avroToProtoConverter)
      throws IOException {
    this.avroStreamWriterProperties = avroStreamWriterProperties;
    this.credentials = credentialsProvider.getCredentials();
    this.threadPoolProvider = threadPoolProvider;
    this.writeClient = BigQueryWriteClient.create(writeSettings);
    this.avroToProtoConverter = avroToProtoConverter;
  }

  public SchemaAwareStreamWriter<GenericRecord> getWriterForStream(String streamName) {
    try {
      ExecutorProvider executorProvider =
          FixedExecutorProvider.create(threadPoolProvider.getExecutorService());

      FlowControlSettings flowControlSettings = FlowControlSettings.newBuilder().build();
      TransportChannelProvider channelProvider =
          BigQueryWriteSettings.defaultGrpcTransportProviderBuilder()
              .setCredentials(credentials)
              .setKeepAliveTime(
                  Duration.ofSeconds(avroStreamWriterProperties.getKeepAliveTimeSeconds()))
              .setKeepAliveWithoutCalls(true)
              .setChannelPoolSettings(
                  ChannelPoolSettings.staticallySized(
                      avroStreamWriterProperties.getChannelPoolStaticSize()))
              .build();
      return SchemaAwareStreamWriter.newBuilder(
              streamName + "/_default", writeClient, avroToProtoConverter)
          .setEnableConnectionPool(true)
          .setExecutorProvider(executorProvider)
          .setFlowControlSettings(flowControlSettings)
          .setChannelProvider(channelProvider)
          .build();
    } catch (Exception e) {
      logger.error("Cannot create SchemaAwareStreamWriter for stream {}", streamName, e);
      throw new RuntimeException(e);
    }
  }
}
