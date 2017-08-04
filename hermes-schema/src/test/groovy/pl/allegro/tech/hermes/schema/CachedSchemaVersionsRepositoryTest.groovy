package pl.allegro.tech.hermes.schema

import com.google.common.util.concurrent.MoreExecutors
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.test.helper.cache.FakeTicker
import spock.lang.Specification

import javax.ws.rs.core.Response
import java.time.Duration

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class CachedSchemaVersionsRepositoryTest extends Specification {

    static final Duration REFRESH_TIME = Duration.ofMinutes(10)
    static final Duration EXPIRE_TIME = Duration.ofMinutes(60)

    static final SchemaVersion v2 = SchemaVersion.valueOf(2)
    static final SchemaVersion v1 = SchemaVersion.valueOf(1)
    static final SchemaVersion v0 = SchemaVersion.valueOf(0)

    def rawSchemaClient = Stub(RawSchemaClient)
    def ticker = new FakeTicker()
    def versionsRepository = new CachedSchemaVersionsRepository(rawSchemaClient, MoreExecutors.sameThreadExecutor(),
            (int) REFRESH_TIME.toMinutes(), (int) EXPIRE_TIME.toMinutes(), ticker)

    def topic = topic("group", "topic").build()

    def "should indicate if schema version exists"() {
        given:
        rawSchemaClient.getVersions(topic.getName()) >> [v1, v0]

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
        rawSchemaClient.getVersions(topic.getName()) >> versions

        expect:
        versionsRepository.latestSchemaVersion(topic).get() == latestVersion

        where:
        versions     | latestVersion
        [v1, v0]     | v1
        [v0, v1]     | v1
        [v1, v2]     | v2
        [v1, v2, v0] | v2
    }

    def "should empty latest schema version for topic without schema versions available"() {
        given:
        rawSchemaClient.getVersions(TopicName.fromQualifiedName("other.topic")) >> []

        expect:
        !versionsRepository.latestSchemaVersion(topic).isPresent()
    }

    def "should respond using cached schema versions if schema versions didn't expire"() {
        given:
        rawSchemaClient.getVersions(topic.getName()) >>> [[v1, v0], [v2, v1, v0]]
        versionsRepository.latestSchemaVersion(topic)
        ticker.advance(REFRESH_TIME.minusMinutes(1))

        expect:
        !versionsRepository.schemaVersionExists(topic, v2)
        versionsRepository.latestSchemaVersion(topic).get() == v1
    }

    def "should respond using fresh schema versions if schema versions did expire"() {
        given:
        rawSchemaClient.getVersions(topic.getName()) >>> [[v1, v0], [v2, v1, v0]]
        versionsRepository.latestSchemaVersion(topic)
        ticker.advance(REFRESH_TIME.plusMinutes(1))

        expect:
        versionsRepository.schemaVersionExists(topic, v2)
        versionsRepository.latestSchemaVersion(topic).get() == v2
    }

    def "should respond with stale data if refresh failed"() {
        given:
        def failing = false
        rawSchemaClient.getVersions(topic.getName()) >> {
            if (failing) {
                throw new InternalSchemaRepositoryException(topic.qualifiedName, Response.serverError().build())
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

    def "should respond with stale data if reload returned empty list"() {
        given:
        def failing = false
        rawSchemaClient.getVersions(topic.getName()) >> { failing? [] : [v1, v0] }
        versionsRepository.latestSchemaVersion(topic)

        ticker.advance(REFRESH_TIME.plusMinutes(1))
        failing = true

        expect:
        versionsRepository.schemaVersionExists(topic, v1)
        versionsRepository.latestSchemaVersion(topic).get() == v1
    }
}
