package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.BufferingResponseListener;
import org.eclipse.jetty.client.Result;

import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.SingleMessageSendingResult;

import java.util.concurrent.CompletableFuture;

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
