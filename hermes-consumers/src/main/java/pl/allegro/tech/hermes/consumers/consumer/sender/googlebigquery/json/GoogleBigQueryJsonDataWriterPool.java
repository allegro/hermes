package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json;

import pl.allegro.tech.hermes.consumers.consumer.sender.SenderClientsPool;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.GoogleBigQuerySenderTarget;

public class GoogleBigQueryJsonDataWriterPool
    extends SenderClientsPool<GoogleBigQuerySenderTarget, GoogleBigQueryJsonDataWriter> {

  private final GoogleBigQueryJsonStreamWriterFactory streamWriterFactory;

  public GoogleBigQueryJsonDataWriterPool(
      GoogleBigQueryJsonStreamWriterFactory streamWriterFactory) {
    this.streamWriterFactory = streamWriterFactory;
  }

  @Override
  protected GoogleBigQueryJsonDataWriter createClient(GoogleBigQuerySenderTarget resolvedTarget) {
    return new GoogleBigQueryJsonDataWriter(
        resolvedTarget.getTableName().toString(), streamWriterFactory);
  }
}
