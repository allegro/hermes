package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import com.google.cloud.bigquery.storage.v1.TableName;
import com.google.common.base.Preconditions;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.EndpointAddress;

public class GoogleBigQuerySenderTargetResolver {

  public static final String GOOGLE_BQ_PROTOCOL = "googlebigquery";
  private static final Logger logger =
      LoggerFactory.getLogger(GoogleBigQuerySenderTargetResolver.class);

  private static final String HOST = "projects";

  GoogleBigQuerySenderTarget resolve(EndpointAddress endpointAddress) {
    try {
      final URI endpointUri = URI.create(endpointAddress.getRawEndpoint());

      logger.info("scheme: {}", endpointUri.getScheme());
      logger.info("host: {}", endpointUri.getHost());
      logger.info("port: {}", endpointUri.getPort());
      logger.info("path: {}", endpointUri.getPath());

      Preconditions.checkArgument(endpointUri.getScheme().equals(GOOGLE_BQ_PROTOCOL));
      Preconditions.checkArgument(endpointUri.getHost().equals(HOST));
      Preconditions.checkArgument(endpointUri.getPort() <= 0);
      final String tableNamePath = endpointUri.getHost() + endpointUri.getPath();

      final TableName tableName = TableName.parse(tableNamePath);

      return GoogleBigQuerySenderTarget.newBuilder().withTableName(tableName).build();
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Given endpoint is invalid", e);
    }
  }
}
