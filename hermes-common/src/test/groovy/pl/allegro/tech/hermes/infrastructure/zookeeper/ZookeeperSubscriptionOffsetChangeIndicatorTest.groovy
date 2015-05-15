package pl.allegro.tech.hermes.infrastructure.zookeeper

import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.domain.subscription.SubscriptionNotExistsException
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffsets
import pl.allegro.tech.hermes.test.IntegrationTest

import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription

class ZookeeperSubscriptionOffsetChangeIndicatorTest extends IntegrationTest {

    private static final String GROUP = 'subscriptionOffsetIndicator'

    private static final TopicName TOPIC = new TopicName(GROUP, 'topic')

    private static final String BROKERS_CLUSTER_NAME = 'primary'

    private ZookeeperSubscriptionOffsetChangeIndicator indicator = new ZookeeperSubscriptionOffsetChangeIndicator(
            zookeeper(), paths, subscriptionRepository);

    void setup() {
        if (!groupRepository.groupExists(GROUP)) {
            groupRepository.createGroup(Group.from(GROUP))
            topicRepository.createTopic(Topic.Builder.topic().withName(TOPIC).build())
        }
    }

    def "should set offsets for subscription to indicate that they should be changed in Kafka"() {
        given:
        subscriptionRepository.createSubscription(subscription().withName('override').withTopicName(TOPIC).build())
        wait.untilSubscriptionCreated(TOPIC, 'override')

        when:
        indicator.setSubscriptionOffset(TOPIC, 'override', BROKERS_CLUSTER_NAME, 1, 10)

        then:
        indicator.getSubscriptionOffsets(TOPIC, 'override', BROKERS_CLUSTER_NAME).forPartition(1) == new PartitionOffset(10, 1)
    }
    
    def "should extract changed offsets"() {
        given:
        subscriptionRepository.createSubscription(subscription().withName('read').withTopicName(TOPIC).build())
        wait.untilSubscriptionCreated(TOPIC, 'read')
        indicator.setSubscriptionOffset(TOPIC, 'read', BROKERS_CLUSTER_NAME, 1, 10)
        
        when:
        PartitionOffsets offsets = indicator.getSubscriptionOffsets(TOPIC, 'read', BROKERS_CLUSTER_NAME)
        
        then:
        offsets.forPartition(1).offset == 10
    }

    def "should throw exception when trying to set offsets for nonexistent subscription"() {
        when:
        indicator.setSubscriptionOffset(TOPIC, 'unknown', BROKERS_CLUSTER_NAME, 1, 10)

        then:
        thrown(SubscriptionNotExistsException)
    }
}
