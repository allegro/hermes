package pl.allegro.tech.hermes.infrastructure.zookeeper


import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffsets
import pl.allegro.tech.hermes.domain.subscription.SubscriptionNotExistsException
import pl.allegro.tech.hermes.test.IntegrationTest

import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.group
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class ZookeeperSubscriptionOffsetChangeIndicatorTest extends IntegrationTest {

    private static final String GROUP = 'subscriptionOffsetIndicator'

    private static final Topic TOPIC = topic(new TopicName(GROUP, 'topic')).build()

    private KafkaTopicName primaryKafkaTopicName

    private ZookeeperSubscriptionOffsetChangeIndicator indicator = new ZookeeperSubscriptionOffsetChangeIndicator(
            zookeeper(), paths, subscriptionRepository)

    void setup() {
        if (!groupRepository.groupExists(GROUP)) {
            groupRepository.createGroup(group(GROUP).build())
            topicRepository.createTopic(TOPIC)
        }
        primaryKafkaTopicName = kafkaNamesMapper.toKafkaTopics(TOPIC).primary.name()
    }

    def "should set offsets for subscription to indicate that they should be changed in Kafka"() {
        given:
        subscriptionRepository.createSubscription(subscription(TOPIC.name, 'override').build())
        wait.untilSubscriptionCreated(TOPIC.name, 'override')

        when:
        indicator.setSubscriptionOffset(TOPIC.name, 'override', 'primary', new PartitionOffset(primaryKafkaTopicName, 10, 1))

        then:
        def offsets = indicator.getSubscriptionOffsets(TOPIC.name, 'override', 'primary', [1] as Set)
        offsets.find { it.partition == 1 } == new PartitionOffset(primaryKafkaTopicName, 10, 1)
    }

    def "should extract changed offsets"() {
        given:
        subscriptionRepository.createSubscription(subscription(TOPIC.name, 'read').build())
        wait.untilSubscriptionCreated(TOPIC.name, 'read')
        indicator.setSubscriptionOffset(TOPIC.name, 'read', 'primary', new PartitionOffset(primaryKafkaTopicName, 10, 1))

        when:
        PartitionOffsets offsets = indicator.getSubscriptionOffsets(TOPIC.name, 'read', 'primary', [1] as Set)

        then:
        (offsets.find { it.partition == 1 } as PartitionOffset).offset == 10
    }

    def "should throw exception when trying to set offsets for nonexistent subscription"() {
        when:
        indicator.setSubscriptionOffset(TOPIC.name, 'unknown', 'primary', new PartitionOffset(primaryKafkaTopicName, 10, 1))

        then:
        thrown(SubscriptionNotExistsException)
    }
}
