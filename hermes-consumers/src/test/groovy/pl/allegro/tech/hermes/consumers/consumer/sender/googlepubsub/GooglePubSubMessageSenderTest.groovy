package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub


import com.google.api.core.SettableApiFuture
import com.google.api.gax.grpc.GrpcStatusCode
import com.google.api.gax.rpc.ApiException
import com.google.cloud.pubsub.v1.Publisher
import com.google.pubsub.v1.PubsubMessage
import io.grpc.Status
import pl.allegro.tech.hermes.common.config.ConfigFactory
import pl.allegro.tech.hermes.consumers.consumer.sender.SingleMessageSendingResult
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.TimeUnit

class GooglePubSubMessageSenderTest extends Specification {

    Publisher publisher = Mock(Publisher)
    GooglePubSubClient client = new GooglePubSubClient(publisher, new GooglePubSubMessages(
            new GooglePubSubMetadataAppender()))

    @Subject
    GooglePubSubMessageSender sender = new GooglePubSubMessageSender(client)

    def 'should return result on a happy path'() {
        given:
        publisher.publish(_ as PubsubMessage) >> apiFuture("test")

        when:
        def result = sender.send(MessageBuilder.testMessage())
                .get(1, TimeUnit.SECONDS)
        then:
        result.succeeded()
    }

    def 'should return failed future when publishing to PubSub failed'() {
        given:
        def exception = new ApiException("not found", null, GrpcStatusCode.of(Status.Code.NOT_FOUND), false)
        publisher.publish(_ as PubsubMessage) >> apiFuture(exception)

        when:
        SingleMessageSendingResult result = sender.send(MessageBuilder.testMessage())
                .get(1, TimeUnit.SECONDS)

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
