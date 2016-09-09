package pl.allegro.tech.hermes.management.infrastructure.audit

import com.fasterxml.jackson.databind.ObjectMapper
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.junit.Before
import org.junit.BeforeClass
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.management.utils.MockAppender
import spock.lang.Specification

import static org.slf4j.Logger.ROOT_LOGGER_NAME

class LoggingAuditorSpec extends Specification {

    static final String TEST_USER = "testUser"
    static final Group TEST_GROUP = new Group("testGroup", "testOwner", "testSupportTeam", "testContact")

    static Logger root = LoggerFactory.getLogger(ROOT_LOGGER_NAME)
    static MockAppender mockAppender = new MockAppender()
    static Javers javers = JaversBuilder.javers().build()
    static ObjectMapper objectMapper = new ObjectMapper()

    @BeforeClass
    def addMockAppender() {
        root.addAppender(mockAppender)
    }

    @Before
    def clearAppender() {
        mockAppender.list.clear()
    }

    def "should log new object creation"() {
        given:
            LoggingAuditor auditor = new LoggingAuditor(javers, objectMapper)

        when:
            auditor.objectCreated(TEST_USER, TEST_GROUP)

        then:
            with(mockAppender.list.last().toString()) {
                it.contains(TEST_USER)
                it.contains(TEST_GROUP.groupName)
            }
    }

    def "should log object removal"() {
        given:
            LoggingAuditor auditor = new LoggingAuditor(javers, objectMapper)

        when:
            auditor.objectRemoved(TEST_USER, TEST_GROUP.getGroupName())

        then:
            with(mockAppender.list.last().toString()) {
                it.contains(TEST_USER)
                it.contains(TEST_GROUP.groupName)
            }
    }

    def "should log object update"() {
        given:
            LoggingAuditor auditor = new LoggingAuditor(javers, objectMapper)
            Group updatedGroup = new Group(TEST_GROUP.groupName, TEST_GROUP.technicalOwner, TEST_GROUP.supportTeam, "updated contact")

        when:
            auditor.objectUpdated(TEST_USER, TEST_GROUP, updatedGroup)

        then:
            with(mockAppender.list.last().toString()) {
                it.contains(TEST_USER)
                it.contains(TEST_GROUP.groupName)
                it.contains(TEST_GROUP.contact)
                it.contains(updatedGroup.contact)
            }
    }
}
