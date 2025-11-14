package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import com.google.cloud.bigquery.storage.v1.TableName;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.avro.generic.GenericRecord;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.GoogleBigQuerySenderTarget;

public class GoogleBigQueryAvroSender implements CompletableFutureAwareMessageSender {

  private final GoogleBigQueryAvroMessageTransformer avroMessageTransformer;
  private final Subscription subscription;
  private final GoogleBigQueryAvroDataWriterPool avroDataWriterPool;

  public GoogleBigQueryAvroSender(
      GoogleBigQueryAvroMessageTransformer avroMessageTransformer,
      Subscription subscription,
      GoogleBigQueryAvroStreamWriterFactory avroStreamWriterFactory) {

    this.avroMessageTransformer = avroMessageTransformer;
    this.subscription = subscription;
    this.avroDataWriterPool = new GoogleBigQueryAvroDataWriterPool(avroStreamWriterFactory);
  }

  @Override
  public void send(Message message, CompletableFuture<MessageSendingResult> resultFuture) {
    GenericRecord record = avroMessageTransformer.fromHermesMessage(message);

    GoogleBigQuerySenderTarget target = getGoogleBigQuerySenderTarget(message);

    try {
      avroDataWriterPool.acquire(target).publish(record, resultFuture);
    } catch (IOException | ExecutionException | InterruptedException e) {
      resultFuture.complete(MessageSendingResult.failedResult(e));
    }
  }

  /** Partition in BigQuery is in the format YYYYMMDD */
  public static String partitionFromTimestamp(long timestampMillis) {
    ZonedDateTime timestamp =
        Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.of("UTC"))
            .withZoneSameInstant(ZoneId.of("Europe/Warsaw"));
    return "%04d%02d%02d"
        .formatted(timestamp.getYear(), timestamp.getMonthValue(), timestamp.getDayOfMonth());
  }

  private GoogleBigQuerySenderTarget getGoogleBigQuerySenderTarget(Message message) {
    String partition = partitionFromTimestamp(message.getPublishingTimestamp());

    TableName wholeTableName =
        TableName.parse(subscription.getEndpoint().getEndpoint().replace("googlebigquery://", ""));
    GoogleBigQuerySenderTarget target =
        GoogleBigQuerySenderTarget.newBuilder()
            .withTableName(
                TableName.of(
                    wholeTableName.getProject(),
                    wholeTableName.getDataset(),
                    wholeTableName.getTable() + "$" + partition))
            .build();
    return target;
  }

  @Override
  public void stop() {
    avroDataWriterPool.shutdown();
  }
}
