package pl.allegro.tech.hermes.frontend.publishing.preview

import pl.allegro.tech.hermes.domain.topic.preview.MessagePreview
import pl.allegro.tech.hermes.frontend.publishing.avro.AvroMessage
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage
import pl.allegro.tech.hermes.frontend.publishing.message.Message
import pl.allegro.tech.hermes.test.helper.avro.AvroUser
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName

class MessagePreviewLogTest extends Specification {

    private MessagePreviewFactory factory = Stub(MessagePreviewFactory) {
        create(_ as Message) >> { args -> new MessagePreview(args[0].data) }
    }

    private MessagePreviewLog log = new MessagePreviewLog(factory, 2)

    def "should persist JSON messages for topics"() {
        given:
        log.add(fromQualifiedName('group.topic-1'), new JsonMessage('id', [1] as byte[], 0L))
        log.add(fromQualifiedName('group.topic-2'), new JsonMessage('id', [2] as byte[], 0L))

        when:
        def messages = log.snapshotAndClean()

        then:
        messages.topics() as Set == [fromQualifiedName('group.topic-1'), fromQualifiedName('group.topic-2')] as Set
    }

    def "should persist Avro messages for topics"() {
        given:
        def avroUser = new AvroUser()
        def message = new AvroMessage('message-id', avroUser.asBytes(), 0L, avroUser.compiledSchema)

        log.add(fromQualifiedName('group.topic-1'), message)

        when:
        def messages = log.snapshotAndClean()

        then:
        messages.topics() as Set == [fromQualifiedName('group.topic-1')] as Set
    }

    def "should persist no more than two messages for topic"() {
        given:
        log.add(fromQualifiedName('group.topic-1'), new JsonMessage('id', [1] as byte[], 0L))
        log.add(fromQualifiedName('group.topic-1'), new JsonMessage('id', [2] as byte[], 0L))
        log.add(fromQualifiedName('group.topic-1'), new JsonMessage('id', [3] as byte[], 0L))

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
                1000.times { log.add(fromQualifiedName("group.topic"), new JsonMessage('id', [executor, it] as byte[], 0L)) }
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

}
