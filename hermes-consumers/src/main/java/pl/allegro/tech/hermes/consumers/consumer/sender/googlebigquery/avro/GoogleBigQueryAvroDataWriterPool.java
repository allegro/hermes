package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import java.io.IOException;
import pl.allegro.tech.hermes.consumers.consumer.sender.SenderClientsPool;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.GoogleBigQuerySenderTarget;

public class GoogleBigQueryAvroDataWriterPool
    extends SenderClientsPool<GoogleBigQuerySenderTarget, GoogleBigQueryAvroDataWriter> {

  private final GoogleBigQueryAvroStreamWriterFactory factory;

  public GoogleBigQueryAvroDataWriterPool(GoogleBigQueryAvroStreamWriterFactory factory) {
    this.factory = factory;
  }

  @Override
  protected GoogleBigQueryAvroDataWriter createClient(GoogleBigQuerySenderTarget resolvedTarget)
      throws IOException {
    return new GoogleBigQueryAvroDataWriter(resolvedTarget.getTableName().toString(), factory);
  }
}
