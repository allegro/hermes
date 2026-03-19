package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import com.google.cloud.bigquery.storage.v1.Exceptions;
import com.google.cloud.bigquery.storage.v1.TableName;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.GoogleBigQuerySenderTarget;

public class GoogleBigQueryAvroSender implements CompletableFutureAwareMessageSender {
  private static final Logger logger = LoggerFactory.getLogger(GoogleBigQueryAvroSender.class);

  private final GoogleBigQueryAvroMessageTransformer avroMessageTransformer;
  private final Subscription subscription;
  private final GoogleBigQueryAvroDataWriterPool avroDataWriterPool;
  private final TableName wholeTableName;

  public GoogleBigQueryAvroSender(
      GoogleBigQueryAvroMessageTransformer avroMessageTransformer,
      Subscription subscription,
      GoogleBigQueryAvroStreamWriterFactory avroStreamWriterFactory) {

    this.avroMessageTransformer = avroMessageTransformer;
    this.subscription = subscription;
    this.avroDataWriterPool = new GoogleBigQueryAvroDataWriterPool(avroStreamWriterFactory);
    this.wholeTableName =
        TableName.parse(subscription.getEndpoint().getEndpoint().replace("googlebigquery://", ""));
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

  @Override
  public void send(Message message, CompletableFuture<MessageSendingResult> resultFuture) {
    GenericRecord record = avroMessageTransformer.fromHermesMessage(message);

    GoogleBigQuerySenderTarget target = getGoogleBigQuerySenderTarget(message, wholeTableName);

    try {
      avroDataWriterPool.acquire(target).publish(record, resultFuture);
    } catch (Exceptions.DataHasUnknownFieldException e) {
      logger.warn(
          "Release writer for target {} due to unknown field in data (schema mismatch with descriptor)",
          target.getTableName(),
          e);
      avroDataWriterPool.reset(target);
      resultFuture.complete(MessageSendingResult.failedResult(e));
    } catch (Exception e) {
      resultFuture.complete(MessageSendingResult.failedResult(e));
    }
  }

  private GoogleBigQuerySenderTarget getGoogleBigQuerySenderTarget(
      Message message, TableName wholeTableName) {
    String partition = partitionFromTimestamp(message.getPublishingTimestamp());

    return GoogleBigQuerySenderTarget.newBuilder()
        .withTableName(
            TableName.of(
                wholeTableName.getProject(),
                wholeTableName.getDataset(),
                wholeTableName.getTable() + "$" + partition))
        .build();
  }

  @Override
  public void stop() {
    avroDataWriterPool.shutdown();
  }
}
