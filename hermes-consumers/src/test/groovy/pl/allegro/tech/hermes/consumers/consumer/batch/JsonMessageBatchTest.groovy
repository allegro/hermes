package pl.allegro.tech.hermes.consumers.consumer.batch

import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.BufferOverflowException
import java.time.Clock

import static java.nio.ByteBuffer.allocate
import static java.nio.ByteBuffer.allocateDirect
import static java.time.Clock.systemDefaultZone
import static pl.allegro.tech.hermes.api.BatchSubscriptionPolicy.Builder.batchSubscriptionPolicy
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class JsonMessageBatchTest extends Specification {

    static def LARGE_BATCH_SIZE = 100
    static def LARGE_BATCH_TIME = Integer.MAX_VALUE
    static def LARGE_BATCH_VOLUME = 1024

    static def BATCH_ID = "1"

    @Unroll
    def "should append data into buffer"() {
        given:
        JsonMessageBatch batch = new JsonMessageBatch(
                BATCH_ID,
                allocate(capacity),
                batchSubscription(LARGE_BATCH_SIZE, LARGE_BATCH_TIME),
                systemDefaultZone()
        )

        when:
        data.each { it -> batch.append(it.bytes, Stub(MessageMetadata)) }
        batch.close();

        then:
        new String(batch.getContent().array()).replaceAll("\\[|\\]", "").split(",") == data

        where:
        data            | capacity
        ['xx']          | 4
        ["x", "x"]      | 5
        ["x", "x", "x"] | 7
    }

    @Unroll
    def "should throw exception when there is no remaining space for given element"() {
        given:
        JsonMessageBatch batch = new JsonMessageBatch(
                BATCH_ID,
                allocateDirect(capacity),
                batchSubscription(LARGE_BATCH_SIZE, LARGE_BATCH_TIME),
                systemDefaultZone()
        )

        when:
        data.each { it -> batch.append(it.bytes, Stub(MessageMetadata)) }
        batch.close();

        then:
        thrown BufferOverflowException

        where:
        data       | capacity
        ["xx"]     | 1
        ["xx"]     | 2
        ["xx"]     | 3
        ["x", "x"] | 4
    }

    @Unroll
    def "should be ready for delivery after ttl exceeded"() {
        given:
        def batchTtl = 1
        def data = "xxx".bytes

        Clock clock = Stub() { millis() >>> [10, 20] }
        JsonMessageBatch jsonMessageBatch = new JsonMessageBatch(
                BATCH_ID,
                allocateDirect(LARGE_BATCH_VOLUME),
                batchSubscription(LARGE_BATCH_SIZE, batchTtl),
                clock
        )

        when:
        jsonMessageBatch.append(data, Stub(MessageMetadata))

        then:
        jsonMessageBatch.isReadyForDelivery()
    }

    @Unroll
    def "should count batchTime since first message appended"() {
        given:
        Integer currentTime = 1;
        Clock clock = Stub() { millis() >> { currentTime } }

        def batchTtl = 10
        JsonMessageBatch jsonMessageBatch = new JsonMessageBatch(
                BATCH_ID,
                allocateDirect(LARGE_BATCH_VOLUME),
                batchSubscription(LARGE_BATCH_SIZE, batchTtl),
                clock
        )
        currentTime = batchTtl + 1

        when:
        jsonMessageBatch.append("xxx".bytes, Stub(MessageMetadata))
        currentTime = batchTtl + 5

        then:
        !jsonMessageBatch.isReadyForDelivery()
    }

    def "should return size and capacity based on the number of bytes appended and allocated"() {
        given:
        JsonMessageBatch message = new JsonMessageBatch(
                BATCH_ID,
                allocateDirect(LARGE_BATCH_VOLUME),
                batchSubscription(LARGE_BATCH_SIZE, 10),
                systemDefaultZone()
        )
        byte [] content = [1, 2, 3]

        when:
        message.append(content, Stub(MessageMetadata))

        then:
        message.getCapacity() == LARGE_BATCH_VOLUME
        message.getSize() == content.length + 1 // an extra byte is added by 'append' to indicate the start of the array

        when:
        message.close()

        then:
        message.getCapacity() == LARGE_BATCH_VOLUME
        message.getSize() == content.length + 2 // an extra byte is added by 'close' to indicate the end of the array
    }

    private static def batchSubscription(int batchSize, int batchTime) {
        def batchSubscriptionPolicy = batchSubscriptionPolicy()
                .withBatchSize(batchSize)
                .withBatchTime(batchTime)
                .build()
        return subscription("group.topic", "subscription").withSubscriptionPolicy(batchSubscriptionPolicy).build()
    }
}
