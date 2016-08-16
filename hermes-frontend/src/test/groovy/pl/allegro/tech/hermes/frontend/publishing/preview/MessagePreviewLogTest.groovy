package pl.allegro.tech.hermes.frontend.publishing.preview

import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName

class MessagePreviewLogTest extends Specification {

    private MessagePreviewLog log = new MessagePreviewLog(2);

    def "should persist messages for topics"() {
        given:
        log.add(fromQualifiedName('group.topic-1'), [1] as byte[])
        log.add(fromQualifiedName('group.topic-2'), [2] as byte[])

        when:
        def messages = log.snapshotAndClean()

        then:
        messages.topics() as Set == [fromQualifiedName('group.topic-1'), fromQualifiedName('group.topic-2')] as Set
    }

    def "should persist no more than two messages for topic"() {
        given:
        log.add(fromQualifiedName('group.topic-1'), [1] as byte[])
        log.add(fromQualifiedName('group.topic-1'), [2] as byte[])
        log.add(fromQualifiedName('group.topic-1'), [3] as byte[])

        when:
        def messages = log.snapshotAndClean()

        then:
        messages.previewOf(fromQualifiedName('group.topic-1')) == [[1] as byte[], [2] as byte[]]
    }

    def "should be thread safe when adding messages for same topic from multiple threads"() {
        given:
        int threads = 10
        CountDownLatch latch = new CountDownLatch(threads)

        ExecutorService executorService = Executors.newFixedThreadPool(threads)
        threads.times {
            int executor = it
            executorService.submit({
                1000.times { log.add(fromQualifiedName("group.topic"), [executor, it] as byte[]) }
                latch.countDown()
            })
        }

        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException('Faild to commit all testing threads within specified timeout')
        }

        when:
        def messages = log.snapshotAndClean()

        then:
        messages.previewOf(fromQualifiedName('group.topic')).size() == 2

        cleanup:
        executorService.shutdown()
    }

}
