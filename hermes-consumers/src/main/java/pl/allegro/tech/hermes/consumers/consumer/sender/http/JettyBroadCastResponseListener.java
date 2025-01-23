package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import java.util.concurrent.CompletableFuture;
import org.eclipse.jetty.client.BufferingResponseListener;
import org.eclipse.jetty.client.Result;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.SingleMessageSendingResult;

public class JettyBroadCastResponseListener extends BufferingResponseListener {

  CompletableFuture<SingleMessageSendingResult> resultFuture;

  public JettyBroadCastResponseListener(
      CompletableFuture<SingleMessageSendingResult> resultFuture) {
    this.resultFuture = resultFuture;
  }

  @Override
  public void onComplete(Result result) {
    resultFuture.complete(MessageSendingResult.of(result));
  }
}
