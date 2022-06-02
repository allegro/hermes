package pl.allegro.tech.hermes.management.domain.consistency

import pl.allegro.tech.hermes.management.config.kafka.KafkaClustersProperties
import pl.allegro.tech.hermes.management.domain.auth.TestRequestUser
import pl.allegro.tech.hermes.management.domain.topic.TopicService
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService
import spock.lang.Specification

class KafkaHermesConsistencyServiceSpec extends Specification {

    TopicService topicService = Stub()
    KafkaClustersProperties kafkaClustersProperties = Stub()
    MultiDCAwareService multiDCAwareService = Mock()

    KafkaHermesConsistencyService kafkaHermesConsistencyService =
            new KafkaHermesConsistencyService(topicService, multiDCAwareService, kafkaClustersProperties)


    def "should list empty list when there is no inconsistent topics"() {
        given:
        topicService.listQualifiedTopicNames() >> [ "pl.allegro.test.FirstTopic",
                                         "pl.allegro.test.SecondTopic"]
        kafkaClustersProperties.getDefaultNamespace() >> ""
        kafkaClustersProperties.getNamespaceSeparator() >> "_"
        multiDCAwareService.listTopicFromAllDC() >> ["pl.allegro.test.FirstTopic_avro",
                                                     "pl.allegro.test.SecondTopic"]

        when:
        def result = kafkaHermesConsistencyService.listInconsistentTopics()

        then:
        result.size() == 0
    }

    def "should list empty list when topics have defined namespace"() {
        given:
        topicService.listQualifiedTopicNames() >> [ "pl.allegro.test.FirstTopic",
                                                    "pl.allegro.test.SecondTopic"]
        kafkaClustersProperties.getDefaultNamespace() >> "namespace"
        kafkaClustersProperties.getNamespaceSeparator() >> "_"
        multiDCAwareService.listTopicFromAllDC() >> ["namespace_pl.allegro.test.FirstTopic_avro",
                                                     "namespace_pl.allegro.test.SecondTopic"]

        when:
        def result = kafkaHermesConsistencyService.listInconsistentTopics()

        then:
        result.size() == 0
    }

    def "should not return ignored topics"() {
        given:
        topicService.listQualifiedTopicNames() >> [ "pl.allegro.test.FirstTopic",
                                                    "pl.allegro.test.SecondTopic"]
        kafkaClustersProperties.getDefaultNamespace() >> "namespace"
        kafkaClustersProperties.getNamespaceSeparator() >> "_"
        multiDCAwareService.listTopicFromAllDC() >> ["namespace_pl.allegro.test.FirstTopic_avro",
                                                     "namespace_pl.allegro.test.SecondTopic",
                                                     "__consumer_offsets"]

        when:
        def result = kafkaHermesConsistencyService.listInconsistentTopics()

        then:
        result.size() == 0
    }

    def "should return topics not present in hermes"() {
        given:
        topicService.listQualifiedTopicNames() >> [ "pl.allegro.test.FirstTopic"]
        kafkaClustersProperties.getDefaultNamespace() >> ""
        kafkaClustersProperties.getNamespaceSeparator() >> "_"
        multiDCAwareService.listTopicFromAllDC() >> ["pl.allegro.test.FirstTopic_avro",
                                                     "pl.allegro.test.SecondTopic"]

        when:
        def result = kafkaHermesConsistencyService.listInconsistentTopics()

        then:
        result.size() == 1
        result.contains("pl.allegro.test.SecondTopic")
    }

    def "should remove topic in kafka cluster"() {
        given:
        def topicName = "pl.allegro.test.FirstTopic_avro"
        def requestUser = new TestRequestUser("username", true)

        when:
        kafkaHermesConsistencyService.removeTopic(topicName, requestUser)

        then:
        1 * multiDCAwareService.removeTopicByName(topicName)
    }
}
