package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import org.junit.Test;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro.GoogleBigQueryAvroSender;

public class GoogleBigQueryAvroSenderTest {
  @Test
  public void shouldProduceRightPartition() {
    // Given
    long timestamp = 1754956797014L; // Example timestamp in milliseconds

    // When
    String partition = GoogleBigQueryAvroSender.partitionFromTimestamp(timestamp);

    // Then
    assert partition.equals("20250812");
  }
}
