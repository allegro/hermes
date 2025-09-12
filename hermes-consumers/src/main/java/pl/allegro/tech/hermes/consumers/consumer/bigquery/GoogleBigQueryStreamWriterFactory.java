package pl.allegro.tech.hermes.consumers.consumer.bigquery;

public interface GoogleBigQueryStreamWriterFactory<T> {

  T getWriterForStream(String streamName);
}
