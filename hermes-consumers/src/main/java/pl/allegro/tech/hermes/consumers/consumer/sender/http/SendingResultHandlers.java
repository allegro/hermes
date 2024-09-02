package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import java.util.concurrent.CompletableFuture;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.Response.CompleteListener;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.SingleMessageSendingResult;

public interface SendingResultHandlers {
  CompleteListener handleSendingResultForSerial(
      CompletableFuture<MessageSendingResult> resultFuture);

  CompleteListener handleSendingResultForBroadcast(
      CompletableFuture<SingleMessageSendingResult> resultFuture);

  MessageSendingResult handleSendingResultForBatch(ContentResponse response);
}
