package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.eclipse.jetty.client.api.Response;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.SingleMessageSendingResult;

import java.util.concurrent.CompletableFuture;

public class DefaultSendingResultHandlersProvider implements SendingResultHandlersProvider {
    @Override
    public Response.CompleteListener provideJettyCompleteListener(CompletableFuture<MessageSendingResult> resultFuture) {
        return new JettyResponseListener(resultFuture);
    }

    @Override
    public Response.CompleteListener provideJettyCompleteListenerForBroadcast(CompletableFuture<SingleMessageSendingResult> resultFuture) {
        return new JettyBroadCastResponseListener(resultFuture);
    }

    @Override
    public MessageSendingResult handleApacheSendingResult(CloseableHttpResponse response) {
        return MessageSendingResult.ofStatusCode(response.getStatusLine().getStatusCode());
    }
}
