package pl.allegro.tech.hermes.management.infrastructure.audit

import com.fasterxml.jackson.databind.ObjectMapper
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.clazz.EntityDefinitionBuilder
import org.junit.Before
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.api.OAuthProvider
import pl.allegro.tech.hermes.management.utils.MockAppender
import pl.allegro.tech.hermes.test.helper.builder.GroupBuilder
import spock.lang.Specification

import static org.slf4j.Logger.ROOT_LOGGER_NAME

class LoggingAuditorSpec extends Specification {

    static final String TEST_USER = "testUser"

    MockAppender mockAppender
    LoggingAuditor auditor = new LoggingAuditor(javers(), new ObjectMapper())

    @Before
    def createAndAddMockAppenderToLogger() {
        Logger root = LoggerFactory.getLogger(ROOT_LOGGER_NAME)
        mockAppender = new MockAppender()
        root.addAppender(mockAppender)
    }

    def "should log new object creation"() {
        given:
            Group toBeCreated = GroupBuilder.group("test-group").build()

        when:
            auditor.objectCreated(TEST_USER, toBeCreated)

        then:
            with(mockAppender.list.last().toString()) {
                it.contains(TEST_USER)
                it.contains(Group.class.getSimpleName())
                it.contains(toBeCreated.groupName)
            }
    }

    def "should log object removal"() {
        given:
            Group toBeRemoved = GroupBuilder.group("testGroup").build()

        when:
            auditor.objectRemoved(TEST_USER, Group.class.getSimpleName(), toBeRemoved.getGroupName())

        then:
            with(mockAppender.list.last().toString()) {
                it.contains(TEST_USER)
                it.contains(toBeRemoved.groupName)
            }
    }

    def "should log object update"() {
        given:
            Group toBeUpdated = GroupBuilder.group("testGroup").build()
            Group updatedGroup = GroupBuilder.group(toBeUpdated.groupName).withContact("updatedContact").build()

        when:
            auditor.objectUpdated(TEST_USER, toBeUpdated, updatedGroup)

        then:
            with(mockAppender.list.last().toString()) {
                it.contains(TEST_USER)
                it.contains(toBeUpdated.groupName)
                it.contains(toBeUpdated.contact)
                it.contains(updatedGroup.contact)
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
