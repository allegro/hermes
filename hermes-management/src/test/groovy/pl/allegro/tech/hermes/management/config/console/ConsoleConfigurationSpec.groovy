package pl.allegro.tech.hermes.management.config.console

import com.fasterxml.jackson.databind.ObjectMapper
import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.management.config.GroupProperties
import pl.allegro.tech.hermes.management.config.TopicProperties
import spock.lang.Specification

class ConsoleConfigurationSpec extends Specification {

    def objectMapper = new ObjectMapper()
    def consoleConfiguration = new ConsoleConfiguration()

    def "should override group settings from GroupProperties"() {
        given: "ConsoleProperties with default group settings"
        def consoleProperties = new ConsoleProperties()
        consoleProperties.group.nonAdminCreationEnabled = false

        and: "GroupProperties with different settings"
        def groupProperties = new GroupProperties()
        groupProperties.nonAdminCreationEnabled = true

        and: "TopicProperties with default content types"
        def topicProperties = new TopicProperties()
        topicProperties.allowedContentTypes = [ContentType.AVRO, ContentType.JSON]

        when: "creating console configuration repository"
        def repository = consoleConfiguration.consoleConfigurationRepository(
                objectMapper, consoleProperties, groupProperties, topicProperties)

        then: "group settings should be overridden from GroupProperties"
        consoleProperties.group.nonAdminCreationEnabled

        and: "repository should be created"
        repository != null
    }

    def "should override content types from TopicProperties"() {
        given: "ConsoleProperties with empty content types"
        def consoleProperties = new ConsoleProperties()
        consoleProperties.topic.contentTypes = []

        and: "GroupProperties with default settings"
        def groupProperties = new GroupProperties()
        groupProperties.nonAdminCreationEnabled = false

        and: "TopicProperties with specific content types"
        def topicProperties = new TopicProperties()
        topicProperties.allowedContentTypes = [ContentType.JSON, ContentType.AVRO]

        when: "creating console configuration repository"
        def repository = consoleConfiguration.consoleConfigurationRepository(
                objectMapper, consoleProperties, groupProperties, topicProperties)

        then: "content types should be overridden from TopicProperties"
        consoleProperties.topic.contentTypes.size() == 2
        consoleProperties.topic.contentTypes*.value == ["JSON", "AVRO"]
        consoleProperties.topic.contentTypes*.label == ["JSON", "AVRO"]

        and: "repository should be created"
        repository != null
    }

    def "should handle single content type from TopicProperties"() {
        given: "ConsoleProperties with default content types"
        def consoleProperties = new ConsoleProperties()

        and: "GroupProperties with default settings"
        def groupProperties = new GroupProperties()

        and: "TopicProperties with only JSON content type"
        def topicProperties = new TopicProperties()
        topicProperties.allowedContentTypes = [ContentType.JSON]

        when: "creating console configuration repository"
        def repository = consoleConfiguration.consoleConfigurationRepository(
                objectMapper, consoleProperties, groupProperties, topicProperties)

        then: "only JSON content type should be set"
        consoleProperties.topic.contentTypes.size() == 1
        consoleProperties.topic.contentTypes[0].value == "JSON"
        consoleProperties.topic.contentTypes[0].label == "JSON"

        and: "repository should be created"
        repository != null
    }

    def "should serialize configuration to JSON correctly"() {
        given: "ConsoleProperties with overridden settings"
        def consoleProperties = new ConsoleProperties()
        def groupProperties = new GroupProperties()
        groupProperties.nonAdminCreationEnabled = true
        def topicProperties = new TopicProperties()
        topicProperties.allowedContentTypes = [ContentType.AVRO]

        when: "creating console configuration repository"
        def repository = consoleConfiguration.consoleConfigurationRepository(
                objectMapper, consoleProperties, groupProperties, topicProperties)

        and: "getting JSON configuration"
        def jsonConfig = repository.configuration

        then: "JSON should contain overridden values"
        jsonConfig != null
        jsonConfig.contains('"nonAdminCreationEnabled":true')
        jsonConfig.contains('"value":"AVRO"')
    }

    def "should always override group settings regardless of console configuration"() {
        given: "ConsoleProperties with nonAdminCreationEnabled = true"
        def consoleProperties = new ConsoleProperties()
        consoleProperties.group.nonAdminCreationEnabled = true

        and: "GroupProperties with nonAdminCreationEnabled = false (source of truth)"
        def groupProperties = new GroupProperties()
        groupProperties.nonAdminCreationEnabled = false

        and: "TopicProperties with default content types"
        def topicProperties = new TopicProperties()
        topicProperties.allowedContentTypes = [ContentType.AVRO, ContentType.JSON]

        when: "creating console configuration repository"
        def repository = consoleConfiguration.consoleConfigurationRepository(
                objectMapper, consoleProperties, groupProperties, topicProperties)

        then: "group settings should be overridden to match GroupProperties (source of truth)"
        !consoleProperties.group.nonAdminCreationEnabled

        and: "repository should be created"
        repository != null
    }
}
