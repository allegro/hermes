package pl.allegro.tech.hermes.management.infrastructure.audit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.clazz.EntityDefinitionBuilder
import org.junit.Before
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.api.OAuthProvider
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.management.utils.MockAppender
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Specification

import static org.slf4j.Logger.ROOT_LOGGER_NAME

class LoggingAuditorSpec extends Specification {

    static final String TEST_USER = "testUser"
    static final String CLIENT_SECRET = "CLIENT_SECRET"
    static final String UPDATED_CLIENT_SECRET = "UPDATED_CLIENT_SECRET"

    MockAppender mockAppender
    LoggingAuditor auditor = new LoggingAuditor(javers(), new ObjectMapper().registerModule(new JavaTimeModule()))

    @Before
    def createAndAddMockAppenderToLogger() {
        Logger root = LoggerFactory.getLogger(ROOT_LOGGER_NAME)
        mockAppender = new MockAppender()
        root.addAppender(mockAppender)
    }

    def "should log new object creation"() {
        given:
            Topic toBeCreated = TopicBuilder.topic("group.topic").build()

        when:
            auditor.objectCreated(TEST_USER, toBeCreated)

        then:
            with(mockAppender.list.last().toString()) {
                it.contains(TEST_USER)
                it.contains(Topic.class.getSimpleName())
                it.contains(toBeCreated.qualifiedName)
            }
    }

    def "should log object removal"() {
        given:
            Topic toBeRemoved = TopicBuilder.topic("group.topic").build()

        when:
            auditor.objectRemoved(TEST_USER, toBeRemoved)

        then:
            with(mockAppender.list.last().toString()) {
                it.contains(TEST_USER)
                it.contains(toBeRemoved.qualifiedName)
            }
    }

    def "should log object update"() {
        given:
            Topic toBeUpdated = TopicBuilder.topic("group.topic").withDescription("some").build()
            Topic updatedTopic = TopicBuilder.topic(toBeUpdated.qualifiedName).withDescription("other").build()

        when:
            auditor.objectUpdated(TEST_USER, toBeUpdated, updatedTopic)

        then:
            with(mockAppender.list.last().toString()) {
                it.contains(TEST_USER)
                it.contains(toBeUpdated.qualifiedName)
                it.contains(toBeUpdated.description)
                it.contains(updatedTopic.description)
            }
    }

    def "should log anonymized data when object is created"() {
        given:
            OAuthProvider toBeCreated = new OAuthProvider("name", "endpoint", "clientId", CLIENT_SECRET, 1, 1, 1, 1)

        when:
            auditor.objectCreated(TEST_USER, toBeCreated)

        then:
            with(mockAppender.list.last().toString()) {
                !it.contains(CLIENT_SECRET)
        }
    }

    def "should log anonymized data when object is updated"() {
        given:
            OAuthProvider toBeUpdated = new OAuthProvider("name", "endpoint", "clientId", CLIENT_SECRET, 1, 1, 1, 1)
            OAuthProvider updated = new OAuthProvider("name", "endpoint", "clientId", UPDATED_CLIENT_SECRET, 1, 1, 1, 1)

        when:
            auditor.objectUpdated(TEST_USER, toBeUpdated, updated)

        then:
            with(mockAppender.list.last().toString()) {
                !it.contains(CLIENT_SECRET)
                !it.contains(UPDATED_CLIENT_SECRET)
        }
    }

    def Javers javers() {
        return JaversBuilder.javers()
                .registerEntity(EntityDefinitionBuilder.entityDefinition(Group.class)
                        .withIdPropertyName("groupName")
                        .build())
                .registerEntity(EntityDefinitionBuilder.entityDefinition(OAuthProvider.class)
                    .withIdPropertyName("name")
                    .build())
                .build()
    }
}
