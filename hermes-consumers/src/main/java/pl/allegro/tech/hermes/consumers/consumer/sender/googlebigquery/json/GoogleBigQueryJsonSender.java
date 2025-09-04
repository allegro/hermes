package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.json;

import org.json.JSONArray;
import org.json.JSONObject;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.GoogleBigQuerySenderTarget;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class GoogleBigQueryJsonSender implements CompletableFutureAwareMessageSender {

    private final GoogleBigQueryJsonMessageTransformer messageTransformer;
    private final GoogleBigQuerySenderTarget senderTarget;
    private final GoogleBigQueryJsonDataWriterPool dataWriterPool;

    public GoogleBigQueryJsonSender(GoogleBigQueryJsonMessageTransformer messageTransformer, GoogleBigQuerySenderTarget senderTarget, GoogleBigQueryJsonDataWriterPool dataWriterPool) {
        this.messageTransformer = messageTransformer;
        this.senderTarget = senderTarget;
        this.dataWriterPool = dataWriterPool;
    }

    @Override
    public void send(Message message, CompletableFuture<MessageSendingResult> resultFuture) {
        JSONObject jsonObject = messageTransformer.fromHermesMessage(message);
        final JSONArray data = new JSONArray();
        data.put(jsonObject);

        try {
            dataWriterPool.acquire(senderTarget)
                    .publish(data, resultFuture);
        } catch (IOException | ExecutionException | InterruptedException e) {
            resultFuture.complete(MessageSendingResult.failedResult(e));
        }
    }

    @Override
    public void stop() {
        dataWriterPool.shutdown();
    }
}
