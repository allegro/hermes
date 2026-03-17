package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.SenderClientsPool;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.GoogleBigQuerySenderTarget;

public class GoogleBigQueryAvroDataWriterPool
    extends SenderClientsPool<GoogleBigQuerySenderTarget, GoogleBigQueryAvroDataWriter> {

  private static final Logger logger = LoggerFactory.getLogger(GoogleBigQueryAvroDataWriterPool.class);
  private final GoogleBigQueryAvroStreamWriterFactory factory;
  public GoogleBigQueryAvroDataWriterPool(GoogleBigQueryAvroStreamWriterFactory factory) {
    this.factory = factory;
  }

  @Override
  protected GoogleBigQueryAvroDataWriter createClient(GoogleBigQuerySenderTarget resolvedTarget)
      throws IOException {
    logger.info("Creating new BigQuery Avro Data Writer for table {}", resolvedTarget.getTableName());
    return new GoogleBigQueryAvroDataWriter(resolvedTarget.getTableName().toString(), factory);
  }

  public void releaseAll(GoogleBigQuerySenderTarget target) {
    while (counters.get(target) > 0) {
      logger.info("Restarting BigQuery Avro Data Writer for table {}. Current counter: {}", target.getTableName(), counters.get(target));
      release(target);
    }
  }
}
