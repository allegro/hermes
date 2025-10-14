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
import java.io.IOException;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Duration;
import pl.allegro.tech.hermes.consumers.config.GoogleBigQueryJsonStreamWriterProperties;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.GoogleBigQueryStreamWriterFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.ThreadPoolProvider;

public class GoogleBigQueryJsonStreamWriterFactory
    implements GoogleBigQueryStreamWriterFactory<JsonStreamWriter> {

  private static final Logger logger = LoggerFactory.getLogger(GoogleBigQueryJsonStreamWriterFactory.class);

  private final GoogleBigQueryJsonStreamWriterProperties jsonStreamWriterProperties;
  private final Credentials credentials;
  private final BigQueryWriteClient writeClient;
  private final ThreadPoolProvider threadPoolProvider;

  public GoogleBigQueryJsonStreamWriterFactory(
      GoogleBigQueryJsonStreamWriterProperties jsonStreamWriterProperties,
      CredentialsProvider credentials,
      ThreadPoolProvider threadPoolProvider,
      GoogleBigQueryJsonWriteClientProvider writeClientProvider)
      throws IOException {
    this.jsonStreamWriterProperties = jsonStreamWriterProperties;
    this.credentials = credentials.getCredentials();
    this.threadPoolProvider = threadPoolProvider;
    this.writeClient = writeClientProvider.getWriteClient();
  }

  public JsonStreamWriter getWriterForStream(String stream) {
    try {
      return JsonStreamWriter.newBuilder(stream, writeClient)
          .setEnableConnectionPool(true)
          .setExecutorProvider(
              FixedExecutorProvider.create(threadPoolProvider.getExecutorService()))
          .setFlowControlSettings(FlowControlSettings.newBuilder().build())
          .setChannelProvider(
              BigQueryWriteSettings.defaultGrpcTransportProviderBuilder()
                  .setCredentials(credentials)
                  .setKeepAliveTime(
                      Duration.ofSeconds(jsonStreamWriterProperties.getKeepAliveTimeSeconds()))
                  .setKeepAliveWithoutCalls(true)
                  .setChannelPoolSettings(
                      ChannelPoolSettings.staticallySized(
                          jsonStreamWriterProperties.getChannelPoolStaticSize()))
                  .build())
          .build();
    } catch (Descriptors.DescriptorValidationException | IOException | InterruptedException e) {
      logger.error("Cannot create JsonStreamWriter for stream {}", stream, e);
      throw new RuntimeException(e);
    }
  }
}
