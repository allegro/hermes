package pl.allegro.tech.hermes.frontend.publishing.preview

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import pl.allegro.tech.hermes.common.message.wrapper.SchemaAwareSerDe
import pl.allegro.tech.hermes.frontend.publishing.avro.AvroMessage
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage
import pl.allegro.tech.hermes.test.helper.avro.AvroUser
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName

class MessagePreviewLogTest extends Specification {

    private MessagePreviewLog log = new MessagePreviewLog(new MessagePreviewFactory(10 * 1024), 2)

    def "should persist JSON messages for topics"() {
        given:
        log.add(TopicBuilder.topic('group.topic-1').build(), new JsonMessage('id', [1] as byte[], 0L, "partition-key"))
        log.add(TopicBuilder.topic('group.topic-2').build(), new JsonMessage('id', [2] as byte[], 0L, null))

        when:
        def messages = log.snapshotAndClean()

        then:
        messages.topics() as Set == [fromQualifiedName('group.topic-1'), fromQualifiedName('group.topic-2')] as Set
    }

    def "should persist Avro messages for topics"() {
        given:
        def avroUser = new AvroUser()
        def message = new AvroMessage('message-id', avroUser.asBytes(), 0L, avroUser.compiledSchema, null)

        log.add(TopicBuilder.topic('group.topic-1').build(), message)

        when:
        def messages = log.snapshotAndClean()

        then:
        messages.topics() as Set == [fromQualifiedName('group.topic-1')] as Set

        def preview = messages.previewOf(fromQualifiedName('group.topic-1')).get(0).content
        def previewString = new String(preview)
        previewString == """{"__metadata":null,"name":"defaultName","age":0,"favoriteColor":"defaultColor"}"""
    }

    def "should persist Avro messages for schema aware topics"() {
        given:
        def avroUser = new AvroUser()
        def message = new AvroMessage('message-id', SchemaAwareSerDe.serialize(avroUser.compiledSchema.id, avroUser.asBytes()), 0L, avroUser.compiledSchema, null)

        log.add(TopicBuilder.topic('group.topic-1').withSchemaIdAwareSerialization().build(), message)

        when:
        def messages = log.snapshotAndClean()

        then:
        messages.topics() as Set == [fromQualifiedName('group.topic-1')] as Set

        def preview = messages.previewOf(fromQualifiedName('group.topic-1'))
        def previewString = new String(preview.get(0).content)
        previewString == """{"__metadata":null,"name":"defaultName","age":0,"favoriteColor":"defaultColor"}"""
    }

    def "should persist no more than two messages for topic"() {
        given:
        log.add(TopicBuilder.topic('group.topic-1').build(), new JsonMessage('id', [1] as byte[], 0L, null))
        log.add(TopicBuilder.topic('group.topic-1').build(), new JsonMessage('id', [2] as byte[], 0L, null))
        log.add(TopicBuilder.topic('group.topic-1').build(), new JsonMessage('id', [3] as byte[], 0L, null))

        when:
        def messages = log.snapshotAndClean()

        then:
        messages.previewOf(fromQualifiedName('group.topic-1'))*.content == [[1] as byte[], [2] as byte[]]
    }

    def "should be thread safe when adding messages for same topic from multiple threads"() {
        given:
        int threads = 10
        CountDownLatch latch = new CountDownLatch(threads)

        ExecutorService executorService = Executors.newFixedThreadPool(threads)
        threads.times {
            int executor = it
            executorService.submit({
                1000.times { log.add(TopicBuilder.topic("group.topic").build(), new JsonMessage('id', [executor, it] as byte[], 0L, null)) }
                latch.countDown()
            })
        }

        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException('Failed to commit all testing threads within specified timeout')
        }

        when:
        def messages = log.snapshotAndClean()

        then:
        messages.previewOf(fromQualifiedName('group.topic')).size() == 2

        cleanup:
        executorService.shutdown()
    }

    static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

}
