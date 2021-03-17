package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.eclipse.jetty.client.api.Response.CompleteListener;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.SingleMessageSendingResult;

import java.util.concurrent.CompletableFuture;

public interface SendingResultHandlersProvider {
    CompleteListener provideJettyCompleteListener(CompletableFuture<MessageSendingResult> resultFuture);
    CompleteListener provideJettyCompleteListenerForBroadcast(CompletableFuture<SingleMessageSendingResult> resultFuture);
    MessageSendingResult handleApacheSendingResult(CloseableHttpResponse response);
}
