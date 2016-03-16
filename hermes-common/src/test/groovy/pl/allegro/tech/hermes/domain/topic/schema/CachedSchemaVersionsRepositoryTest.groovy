package pl.allegro.tech.hermes.domain.topic.schema

import com.google.common.base.Ticker
import com.google.common.util.concurrent.MoreExecutors
import spock.lang.Specification

import java.time.Duration

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class CachedSchemaVersionsRepositoryTest extends Specification {

    static final Duration REFRESH_TIME = Duration.ofMinutes(10)
    static final Duration EXPIRE_TIME = Duration.ofMinutes(60)

    static final SchemaVersion v2 = SchemaVersion.valueOf(2)
    static final SchemaVersion v1 = SchemaVersion.valueOf(1)
    static final SchemaVersion v0 = SchemaVersion.valueOf(0)

    def schemaSourceProvider = Stub(SchemaSourceProvider)
    def ticker = new FakeTicker()
    def versionsRepository = new CachedSchemaVersionsRepository(schemaSourceProvider, MoreExecutors.sameThreadExecutor(),
            (int) REFRESH_TIME.toMinutes(), (int) EXPIRE_TIME.toMinutes(), ticker)

    def topic = topic("group", "topic").build()

    def "should indicate if schema version exists"() {
        given:
        schemaSourceProvider.versions(topic) >> [v1, v0]

        expect:
        versionsRepository.schemaVersionExists(topic, version) == expectedResult

        where:
        version | expectedResult
        v2      | false
        v1      | true
        v0      | true
    }

    def "should provide latest schema version"() {
        given:
        schemaSourceProvider.versions(topic) >> [v1, v0]

        expect:
        versionsRepository.latestSchemaVersion(topic).get() == v1
    }

    def "should empty latest schema version for topic without schema versions available"() {
        given:
        schemaSourceProvider.versions(topic("other", "topic").build()) >> []

        expect:
        !versionsRepository.latestSchemaVersion(topic).isPresent()
    }

    def "should respond using cached schema versions if schema versions didn't expire"() {
        given:
        schemaSourceProvider.versions(topic) >>> [[v1, v0], [v2, v1, v0]]
        versionsRepository.latestSchemaVersion(topic)
        ticker.advance(REFRESH_TIME.minusMinutes(1))

        expect:
        !versionsRepository.schemaVersionExists(topic, v2)
        versionsRepository.latestSchemaVersion(topic).get() == v1
    }

    def "should respond using fresh schema versions if schema versions did expire"() {
        given:
        schemaSourceProvider.versions(topic) >>> [[v1, v0], [v2, v1, v0]]
        versionsRepository.latestSchemaVersion(topic)
        ticker.advance(REFRESH_TIME.plusMinutes(1))

        expect:
        versionsRepository.schemaVersionExists(topic, v2)
        versionsRepository.latestSchemaVersion(topic).get() == v2
    }

    def "should respond with stale data if refresh failed"() {
        given:
        def failing = false
        schemaSourceProvider.versions(topic) >> {
            if (failing) {
                throw new RuntimeException("failing mode on")
            }
            return [v1, v0]
        }
        versionsRepository.latestSchemaVersion(topic)

        ticker.advance(REFRESH_TIME.plusMinutes(1))
        failing = true

        expect:
        versionsRepository.schemaVersionExists(topic, v1)
        versionsRepository.latestSchemaVersion(topic).get() == v1
    }

    private static class FakeTicker extends Ticker {

        private long currentNanos = 0

        @Override
        public long read() {
            return currentNanos
        }

        public void advance(Duration duration) {
            currentNanos += duration.toNanos()
        }

    }

}
