package pl.allegro.tech.hermes.infrastructure.zookeeper

import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffsets
import pl.allegro.tech.hermes.domain.subscription.SubscriptionNotExistsException
import pl.allegro.tech.hermes.test.IntegrationTest

import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription
import static pl.allegro.tech.hermes.api.Topic.Builder.topic

class ZookeeperSubscriptionOffsetChangeIndicatorTest extends IntegrationTest {

    private static final String GROUP = 'subscriptionOffsetIndicator'

    private static final TopicName TOPIC_NAME = new TopicName(GROUP, 'topic')

    private static final Topic TOPIC = topic().applyDefaults().withContentType(ContentType.JSON).withName(TOPIC_NAME).build()

    private static final String BROKERS_CLUSTER_NAME = 'primary'

    private KafkaTopicName primaryKafkaTopicName

    private ZookeeperSubscriptionOffsetChangeIndicator indicator = new ZookeeperSubscriptionOffsetChangeIndicator(
            zookeeper(), paths, subscriptionRepository)

    void setup() {
        if (!groupRepository.groupExists(GROUP)) {
            groupRepository.createGroup(Group.from(GROUP))
            topicRepository.createTopic(TOPIC)
        }
        primaryKafkaTopicName = kafkaNamesMapper.toKafkaTopics(TOPIC).primary.name()
    }

    def "should set offsets for subscription to indicate that they should be changed in Kafka"() {
        given:
        subscriptionRepository.createSubscription(subscription().withName('override').withTopicName(TOPIC_NAME).build())
        wait.untilSubscriptionCreated(TOPIC_NAME, 'override')

        when:
        indicator.setSubscriptionOffset(TOPIC_NAME, 'override', BROKERS_CLUSTER_NAME, new PartitionOffset(primaryKafkaTopicName, 10, 1))

        then:
        def offsets = indicator.getSubscriptionOffsets(TOPIC, 'override', BROKERS_CLUSTER_NAME)
        offsets.find { it.partition == 1 } == new PartitionOffset(primaryKafkaTopicName, 10, 1)
    }

    def "should extract changed offsets"() {
        given:
        subscriptionRepository.createSubscription(subscription().withName('read').withTopicName(TOPIC_NAME).build())
        wait.untilSubscriptionCreated(TOPIC_NAME, 'read')
        indicator.setSubscriptionOffset(TOPIC_NAME, 'read', BROKERS_CLUSTER_NAME, new PartitionOffset(primaryKafkaTopicName, 10, 1))

        when:
        PartitionOffsets offsets = indicator.getSubscriptionOffsets(TOPIC, 'read', BROKERS_CLUSTER_NAME)

        then:
        (offsets.find { it.partition == 1 } as PartitionOffset).offset == 10
    }

    def "should throw exception when trying to set offsets for nonexistent subscription"() {
        when:
        indicator.setSubscriptionOffset(TOPIC_NAME, 'unknown', BROKERS_CLUSTER_NAME, new PartitionOffset(primaryKafkaTopicName, 10, 1))

        then:
        thrown(SubscriptionNotExistsException)
    }
}
