package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import java.util.concurrent.CompletableFuture;

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
