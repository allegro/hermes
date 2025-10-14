package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

public interface GoogleBigQueryStreamWriterFactory<T> {

  T getWriterForStream(String streamName);
}
