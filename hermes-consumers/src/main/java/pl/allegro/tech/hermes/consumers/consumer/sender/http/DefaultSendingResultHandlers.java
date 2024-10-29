package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import java.util.concurrent.CompletableFuture;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.Response;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.SingleMessageSendingResult;

public class DefaultSendingResultHandlers implements SendingResultHandlers {
  @Override
  public Response.CompleteListener handleSendingResultForSerial(
      CompletableFuture<MessageSendingResult> resultFuture) {
    return new JettyResponseListener(resultFuture);
  }

  @Override
  public Response.CompleteListener handleSendingResultForBroadcast(
      CompletableFuture<SingleMessageSendingResult> resultFuture) {
    return new JettyBroadCastResponseListener(resultFuture);
  }

  @Override
  public MessageSendingResult handleSendingResultForBatch(ContentResponse response) {
    return MessageSendingResult.ofStatusCode(response.getStatus());
  }
}
