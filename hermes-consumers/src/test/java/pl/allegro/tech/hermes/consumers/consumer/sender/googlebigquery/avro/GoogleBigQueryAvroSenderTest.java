package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import org.junit.Test;

public class GoogleBigQueryAvroSenderTest {
  @Test
  public void shouldProduceRightPartition() {
    // given
    long timestamp = 1754956797014L; // Example timestamp in milliseconds

    // when
    String partition = GoogleBigQueryAvroSender.partitionFromTimestamp(timestamp);

    // then
    assert partition.equals("20250812");
  }
}
