package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import java.util.concurrent.CompletableFuture;
import org.eclipse.jetty.client.BufferingResponseListener;
import org.eclipse.jetty.client.Result;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

public class JettyResponseListener extends BufferingResponseListener {

  CompletableFuture<MessageSendingResult> resultFuture;

  public JettyResponseListener(CompletableFuture<MessageSendingResult> resultFuture) {
    this.resultFuture = resultFuture;
  }

  @Override
  public void onComplete(Result result) {
    resultFuture.complete(MessageSendingResult.of(result));
  }
}
