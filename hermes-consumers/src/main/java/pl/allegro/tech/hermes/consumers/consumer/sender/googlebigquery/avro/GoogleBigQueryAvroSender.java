package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import org.apache.avro.generic.GenericRecord;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.GoogleBigQuerySenderTarget;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class GoogleBigQueryAvroSender implements CompletableFutureAwareMessageSender {

    private final GoogleBigQueryAvroMessageTransformer avroMessageTransformer;
    private final GoogleBigQuerySenderTarget target;
    private final GoogleBigQueryAvroDataWriterPool avroDataWriterPool;

    public GoogleBigQueryAvroSender(GoogleBigQueryAvroMessageTransformer avroMessageTransformer,
                                    GoogleBigQuerySenderTarget target,
                                    GoogleBigQueryAvroDataWriterPool avroDataWriterPool) {

        this.avroMessageTransformer = avroMessageTransformer;
        this.target = target;
        this.avroDataWriterPool = avroDataWriterPool;
    }

    @Override
    public void send(Message message, CompletableFuture<MessageSendingResult> resultFuture) {
        GenericRecord record = avroMessageTransformer.fromHermesMessage(message);

        try {
            avroDataWriterPool.acquire(target).publish(record, resultFuture);
        } catch (IOException | ExecutionException | InterruptedException e) {
            resultFuture.complete(MessageSendingResult.failedResult(e));
        }
    }

    @Override
    public void stop() {
        avroDataWriterPool.shutdown();
    }
}
