package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub

import com.google.api.core.SettableApiFuture
import com.google.api.gax.grpc.GrpcStatusCode
import com.google.api.gax.rpc.ApiException
import com.google.cloud.pubsub.v1.Publisher
import com.google.pubsub.v1.PubsubMessage
import com.google.pubsub.v1.TopicName
import io.grpc.Status
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class GooglePubSubMessageSenderTest extends Specification {

    Publisher publisher = Mock(Publisher)

    GooglePubSubSenderTarget senderTarget = GooglePubSubSenderTarget.builder()
            .withTopicName(TopicName.of("test-project", "topic-name"))
            .build()

    GooglePubSubClientsPool clientsPool = Mock(GooglePubSubClientsPool)

    GooglePubSubClient client = new GooglePubSubClient(publisher, new GooglePubSubMessageTransformerRaw(
            new GooglePubSubMetadataAppender()))

    @Subject
    GooglePubSubMessageSender sender

    void setup() {
        clientsPool.acquire(senderTarget) >> client
        sender = new GooglePubSubMessageSender(senderTarget, clientsPool)
    }

    def 'should return result on a happy path'() {
        given:
        publisher.publish(_ as PubsubMessage) >> apiFuture("test")

        when:
        CompletableFuture<MessageSendingResult> future = new CompletableFuture();
        sender.send(MessageBuilder.testMessage(), future)
        MessageSendingResult result = future.get(1, TimeUnit.SECONDS)

        then:
        result.succeeded()
    }

    def 'should return failed future when publishing to PubSub failed'() {
        given:
        def exception = new ApiException("not found", null, GrpcStatusCode.of(Status.Code.NOT_FOUND), false)
        publisher.publish(_ as PubsubMessage) >> apiFuture(exception)

        when:
        CompletableFuture<MessageSendingResult> future = new CompletableFuture();
        sender.send(MessageBuilder.testMessage(), future)
        MessageSendingResult result = future.get(1, TimeUnit.SECONDS)

        then:
        !result.succeeded()
        result.getRootCause() == "not found"
    }

    def apiFuture(String messageId) {
        def future = SettableApiFuture.create()
        future.set(messageId)
        future
    }

    def apiFuture(Throwable ex) {
        def future = SettableApiFuture.create()
        future.setException(ex)
        future
    }
}