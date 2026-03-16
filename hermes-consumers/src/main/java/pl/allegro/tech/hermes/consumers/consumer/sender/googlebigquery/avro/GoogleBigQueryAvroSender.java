package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import com.google.cloud.bigquery.storage.v1.TableName;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.FieldMissingInDescriptorException;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.GoogleBigQueryFailedAppendException;
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
    } catch (FieldMissingInDescriptorException e) {
      logger.warn("Release writer for target {} due to missing field in descriptor", target, e);
      resultFuture.complete(
          MessageSendingResult.failedResult(new GoogleBigQueryFailedAppendException(e)));
      avroDataWriterPool.release(getGoogleBigQuerySenderTarget(message, wholeTableName));
    } catch (IOException | ExecutionException | InterruptedException e) {
      resultFuture.complete(MessageSendingResult.failedResult(e));
      resultFuture.complete(
          MessageSendingResult.failedResult(new GoogleBigQueryFailedAppendException(e)));
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
