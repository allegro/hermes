package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response.CompleteListener;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.SingleMessageSendingResult;

import java.util.concurrent.CompletableFuture;

public interface SendingResultHandlers {
    CompleteListener handleSendingResultForSerial(CompletableFuture<MessageSendingResult> resultFuture);

    CompleteListener handleSendingResultForBroadcast(CompletableFuture<SingleMessageSendingResult> resultFuture);

    MessageSendingResult handleSendingResultForBatch(ContentResponse response);
}
