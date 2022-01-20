package pl.allegro.tech.hermes.consumers.consumer.sender.pubsub;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Header;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.succeededResult;

public class PubSubClient {

    private static final Logger logger = LoggerFactory.getLogger(PubSubClient.class);

    private final String projectId;
    private final String topicId;

    PubSubClient(String projectId, String topicId) {
        this.projectId = projectId;
        this.topicId = topicId;
    }

    public void publishingMessage(Message message, CompletableFuture<MessageSendingResult> resultFuture) throws IOException, ExecutionException, InterruptedException {
        TopicName topicName = TopicName.of(this.projectId, this.topicId);
        Publisher publisher = null;
        try {
            publisher = Publisher.newBuilder(topicName).build();
            PubsubMessage pubsubMessage = createPubSubMessage(message);
            ApiFuture<String> future = publisher.publish(pubsubMessage);
            ApiFutures.addCallback(future, pubsubCallback(message, resultFuture), MoreExecutors.directExecutor());
        } finally {
            if (publisher != null) {
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
    }

    private PubsubMessage createPubSubMessage(Message message) {
        return PubsubMessage.newBuilder()
                .setData(ByteString.copyFrom(message.getData()))
                .putAllAttributes(ImmutableMap.of("topic", message.getTopic()))
                .putAllAttributes(prepareHeadersFromMessage(message))
                .putAllAttributes(message.getExternalMetadata())
                .build();
    }

    private Map<String, String> prepareHeadersFromMessage(Message message) {
        return message.getAdditionalHeaders().stream()
                .collect(Collectors.toMap(Header::getName, Header::getValue));
    }

    private ApiFutureCallback<String> pubsubCallback(Message message, CompletableFuture<MessageSendingResult> resultFuture) {
        return new ApiFutureCallback<String>() {
            @Override
            public void onFailure(Throwable throwable) {
                StringBuilder exceptionMessage = new StringBuilder("Error while publishing message to PubSub: ");
                exceptionMessage.append(message);
                if (throwable instanceof ApiException) {
                    ApiException apiException = ((ApiException) throwable);
                    exceptionMessage.append(apiException.getStatusCode().getCode());
                    exceptionMessage.append(apiException.isRetryable());
                }
                logger.info(exceptionMessage.toString());
                resultFuture.complete(failedResult(throwable));
            }

            @Override
            public void onSuccess(String messageId) {
                resultFuture.complete(succeededResult());
                logger.info("Published message ID to PubSub: " + messageId);
            }
        };
    }
}
