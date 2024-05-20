package pl.allegro.tech.hermes.frontend.producer.kafka

import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.ProducerRecord
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.frontend.metric.CachedTopic
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage
import pl.allegro.tech.hermes.frontend.publishing.message.Message
import pl.allegro.tech.hermes.frontend.readiness.AdminReadinessService
import pl.allegro.tech.hermes.frontend.server.CachedTopicsTestHelper
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicInteger

import static java.util.Collections.emptyMap

class MultiDatacenterMessageProducerTest extends Specification {
    ScheduledExecutorService fallbackScheduler = Executors.newSingleThreadScheduledExecutor();
    ExecutorService testExecutor = Executors.newSingleThreadExecutor();


    Duration speculativeSendDelay = Duration.ofMillis(250);
    Message message = new JsonMessage('id', [2] as byte[], 0L, null, emptyMap());

    private static final String localDC = "LOCAL"
    private static final String remoteDC = "REMOTE"

    class VerificationCallback implements PublishingCallback {
        AtomicInteger onUnpublished = new AtomicInteger(0)
        AtomicInteger onPublished = new AtomicInteger(0)
        AtomicInteger publishedToLocal = new AtomicInteger(0)
        AtomicInteger publishedToRemote = new AtomicInteger(0)
        volatile String exception = ""

        @Override
        void onUnpublished(Message message, Topic topic, Exception exception) {
            onUnpublished.incrementAndGet();
            this.exception = exception;
        }

        @Override
        void onPublished(Message message, Topic topic) {
            onPublished.incrementAndGet();
        }

        @Override
        void onEachPublished(Message message, Topic topic, String datacenter) {
            if (datacenter == localDC) {
                publishedToLocal.incrementAndGet()
            } else if (datacenter == remoteDC) {
                publishedToRemote.incrementAndGet()
            }
        }
    }

    def "should send to local DC only if the send is successful before speculativeSendDelay elapses"() {
        given:
        CachedTopic cachedTopic = CachedTopicsTestHelper.cachedTopic(TopicBuilder.topicWithRandomName().build())

        def localSender = successfulSender(localDC)
        def remoteSender = successfulSender(remoteDC)

        def producer = producer(localSender, remoteSender)

        PublishingCallback callback = new VerificationCallback();

        when:
        testExecutor.execute { producer.send(message, cachedTopic, callback) }

        then:
        // wait until speculative send delay elapses
        Thread.sleep(speculativeSendDelay.plusMillis(200).toMillis())
        callback.publishedToLocal.get() == 1
        callback.publishedToRemote.get() == 0
        callback.onPublished.get() == 1
        callback.onUnpublished.get() == 0

    }

    def "should send to remote DC if local DC fails"() {
        given:
        CachedTopic cachedTopic = CachedTopicsTestHelper.cachedTopic(TopicBuilder.topicWithRandomName().build())

        def localSender = failingSender(localDC)
        def remoteSender = successfulSender(remoteDC)

        def producer = producer(localSender, remoteSender)


        PublishingCallback callback = new VerificationCallback();

        when:
        testExecutor.execute { producer.send(message, cachedTopic, callback) }

        then:
        new PollingConditions(timeout: 10).eventually {
            callback.publishedToLocal.get() == 0
            callback.publishedToRemote.get() == 1
            callback.onPublished.get() == 1
            callback.onUnpublished.get() == 0
        }
    }

    def "should send to remote DC when local does not respond before speculativeSendDelay elapses"() {
        given:
        CachedTopic cachedTopic = CachedTopicsTestHelper.cachedTopic(TopicBuilder.topicWithRandomName().build())

        def localSender = stuckLocalSender()
        def remoteSender = successfulSender(remoteDC)

        def producer = producer(localSender, remoteSender)

        PublishingCallback callback = new VerificationCallback();

        when:
        def start = System.currentTimeMillis()
        testExecutor.execute { producer.send(message, cachedTopic, callback) }

        then:
        new PollingConditions(timeout: 10).eventually {
            callback.publishedToLocal.get() == 0
            callback.publishedToRemote.get() == 1
            callback.onPublished.get() == 1
            callback.onUnpublished.get() == 0
            System.currentTimeMillis() - start > speculativeSendDelay.toMillis()
        }
    }

    def "should publish to local DC and remote DC when local send is slower than speculativeSendDelay but it eventually succeeds"() {
        given:
        CachedTopic cachedTopic = CachedTopicsTestHelper.cachedTopic(TopicBuilder.topicWithRandomName().build())

        def localSender = delayedSender(localDC, speculativeSendDelay.plusMillis(100).toMillis())
        def remoteSender = successfulSender(remoteDC)

        def producer = producer(localSender, remoteSender)

        PublishingCallback callback = new VerificationCallback();

        when:
        testExecutor.execute { producer.send(message, cachedTopic, callback) }

        then:
        new PollingConditions(timeout: 10).eventually {
            callback.publishedToLocal.get() == 1
            callback.publishedToRemote.get() == 1
            callback.onPublished.get() == 1
            callback.onUnpublished.get() == 0
        }
    }

    def "should invoke onUnpublished when both DCs fail"() {
        given:
        CachedTopic cachedTopic = CachedTopicsTestHelper.cachedTopic(TopicBuilder.topicWithRandomName().build())

        def localSender = failingSender(localDC, "network error")
        def remoteSender = failingSender(remoteDC, "not leader or follower")

        def producer = producer(localSender, remoteSender)


        PublishingCallback callback = new VerificationCallback();

        when:
        testExecutor.execute { producer.send(message, cachedTopic, callback) }

        then:
        new PollingConditions(timeout: 10).eventually {
            callback.publishedToLocal.get() == 0
            callback.publishedToRemote.get() == 0
            callback.onPublished.get() == 0
            callback.onUnpublished.get() == 1
        }
        callback.exception.contains("[LOCAL]: RuntimeException: network error")
        callback.exception.contains("[REMOTE]: RuntimeException: not leader or follower")
    }

    def "should publish to remote DC once when both scheduled fallback (after speculativeSendDelay) and immediate fallback are run"() {
        given:
        CachedTopic cachedTopic = CachedTopicsTestHelper.cachedTopic(TopicBuilder.topicWithRandomName().build())

        def localSender = delayedFailingSender(localDC, speculativeSendDelay.toMillis() + 5)
        def remoteSender = successfulSender(remoteDC)

        def producer = producer(localSender, remoteSender)

        PublishingCallback callback = new VerificationCallback()

        when:
        testExecutor.execute { producer.send(message, cachedTopic, callback) }

        then:
        Thread.sleep(speculativeSendDelay.toMillis() + 100)
        callback.publishedToLocal.get() == 0
        callback.publishedToRemote.get() == 1
        callback.onPublished.get() == 1
        callback.onUnpublished.get() == 0
    }


    KafkaMessageSender stuckLocalSender() {
        return Mock(KafkaMessageSender) {
            getDatacenter() >> localDC
        }
    }

    KafkaMessageSender delayedSender(String sender, long delayMillis) {
        return Mock(KafkaMessageSender) {
            getDatacenter() >> sender
            send(*_) >> (arguments) -> {
                Thread.sleep(delayMillis)
                callback(arguments).onCompletion(null, null)
            }
        }
    }

    KafkaMessageSender delayedFailingSender(String sender, long delayMillis, String exceptionMsg = "fail") {
        return Mock(KafkaMessageSender) {
            getDatacenter() >> sender
            send(*_) >> (arguments) -> {
                Thread.sleep(delayMillis)
                callback(arguments).onCompletion(null, new RuntimeException(exceptionMsg))
            }
        }
    }

    KafkaMessageSender failingSender(String sender, String exceptionMsg = "fail") {
        return Mock(KafkaMessageSender) {
            getDatacenter() >> sender
            send(*_) >> {
                arguments -> callback(arguments).onCompletion(null, new RuntimeException(exceptionMsg))
            }
        }
    }

    KafkaMessageSender successfulSender(String sender) {
        return Mock(KafkaMessageSender) {
            getDatacenter() >> sender
            send(*_) >> {
                arguments -> callback(arguments).onCompletion(null, null)
            }
        }
    }

    // callback argument from pl.allegro.tech.hermes.frontend.producer.kafka.KafkaMessageSender#send
    Callback callback(arguments) {
        return (arguments[3] as Callback)
    }

    MultiDatacenterMessageProducer producer(KafkaMessageSender localSender, KafkaMessageSender remoteSender) {
        MessageToKafkaProducerRecordConverter messageConverter = Mock(MessageToKafkaProducerRecordConverter) {
            convertToProducerRecord(*_) >> new ProducerRecord("topic", new byte[]{0x0}, new byte[]{0x0})
        }

        def adminReadinessService = Mock(AdminReadinessService) {
            isDatacenterReady(_) >> true
        }

        KafkaMessageSenders senders = new KafkaMessageSenders(
                Mock(TopicMetadataLoadingExecutor),
                Mock(MinInSyncReplicasLoader),
                new KafkaMessageSenders.Tuple(
                        localSender, localSender
                ),
                [new KafkaMessageSenders.Tuple(remoteSender, remoteSender)]
        )

        return new MultiDatacenterMessageProducer(
                senders, adminReadinessService, messageConverter, speculativeSendDelay, fallbackScheduler
        )
    }
}
