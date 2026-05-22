package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json;

import com.google.cloud.bigquery.storage.v1.Exceptions;
import java.util.concurrent.CompletableFuture;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.GoogleBigQuerySenderTarget;

public class GoogleBigQueryJsonSender implements CompletableFutureAwareMessageSender {

  private static final Logger logger = LoggerFactory.getLogger(GoogleBigQueryJsonSender.class);

  private final GoogleBigQueryJsonMessageTransformer messageTransformer;
  private final GoogleBigQuerySenderTarget senderTarget;
  private final GoogleBigQueryJsonDataWriterPool jsondataWriterPool;

  public GoogleBigQueryJsonSender(
      GoogleBigQueryJsonMessageTransformer messageTransformer,
      GoogleBigQuerySenderTarget senderTarget,
      GoogleBigQueryJsonStreamWriterFactory jsonStreamWriterFactory) {
    this.messageTransformer = messageTransformer;
    this.senderTarget = senderTarget;
    this.jsondataWriterPool = new GoogleBigQueryJsonDataWriterPool(jsonStreamWriterFactory);
  }

  @Override
  public void send(Message message, CompletableFuture<MessageSendingResult> resultFuture) {
    JSONObject jsonObject = messageTransformer.fromHermesMessage(message);
    final JSONArray data = new JSONArray();
    data.put(jsonObject);

    try {
      jsondataWriterPool.acquire(senderTarget).publish(data, resultFuture);
    } catch (Exceptions.DataHasUnknownFieldException e) {
      logger.warn(
          "Resetting BigQuery JSON data writer for target {} due to unknown field in data. "
              + "This may indicate a schema change.",
          senderTarget,
          e);
      jsondataWriterPool.reset(senderTarget);
      resultFuture.complete(MessageSendingResult.failedResult(e));
    } catch (Exception e) {
      resultFuture.complete(MessageSendingResult.failedResult(e));
    }
  }

  @Override
  public void stop() {
    jsondataWriterPool.shutdown();
  }
}
