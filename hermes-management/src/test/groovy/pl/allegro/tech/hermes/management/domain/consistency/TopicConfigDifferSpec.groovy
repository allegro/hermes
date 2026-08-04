package pl.allegro.tech.hermes.management.domain.consistency

import org.apache.kafka.common.config.TopicConfig
import pl.allegro.tech.hermes.management.config.TopicProperties
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.OwnedTopicConfig
import spock.lang.Specification

class TopicConfigDifferSpec extends Specification {

    private final TopicConfigDiffer differ = new TopicConfigDiffer()
    private final Map<String, String> desired = [
            (TopicConfig.RETENTION_MS_CONFIG): "86400000",
            (TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG): "false",
            (TopicConfig.MAX_MESSAGE_BYTES_CONFIG): "1048576"
    ]

    def "should normalize owned config values"() {
        expect:
        differ.diff(desired, [
                (TopicConfig.RETENTION_MS_CONFIG): " 86400000 ",
                (TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG): "FALSE",
                (TopicConfig.MAX_MESSAGE_BYTES_CONFIG): "1048576"
        ]).isEmpty()
    }

    def "should report missing and invalid owned config values"() {
        when:
        def result = differ.diff(desired, [
                (TopicConfig.RETENTION_MS_CONFIG): "invalid",
                (TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG): "invalid",
                (TopicConfig.MAX_MESSAGE_BYTES_CONFIG): "1048576"
        ])

        then:
        result*.key() == [TopicConfig.RETENTION_MS_CONFIG,
                          TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG]
        result[0].actual() == "invalid"
        result[1].actual() == "invalid"
    }

    def "should derive exactly the configs owned by Hermes"() {
        given:
        def properties = new TopicProperties()
        properties.setUncleanLeaderElectionEnabled(true)
        properties.setMaxMessageSize(2048)

        expect:
        OwnedTopicConfig.desiredConfig(1234, properties) == [
                (TopicConfig.RETENTION_MS_CONFIG): "1234",
                (TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG): "true",
                (TopicConfig.MAX_MESSAGE_BYTES_CONFIG): "2048"
        ]
    }
}
