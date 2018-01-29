package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import java.nio.charset.StandardCharsets
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset
import pl.allegro.tech.hermes.test.IntegrationTest
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperAssertions


class SetSubscriptionOffsetZookeeperCommandTest extends IntegrationTest {

    def client = zookeeperClient()
    def assertions = new ZookeeperAssertions(client.getCuratorFramework(), mapper)

    def setup() {
        createGroupIfNotExists("group")
        createTopicIfNotExists("topic", "group")
        createSubscriptionIfNotExists("subscription", "topic", "group")
    }

    def "should set subscription offset"() {
        given:
        def path = "/hermes/groups/group/topics/topic/subscriptions/subscription/kafka_topics/kafkaTopic/offset/cluster/0"

        and:
        def command = commandFactory.setSubscriptionOffset(offsetData(42L, 0))

        when:
        command.execute(client)

        then:
        wait.untilZookeeperPathIsCreated(path)
        assertions.zookeeperPathContains(path, 42L)
    }

    def "should rollback subscription offset creation"() {
        given:
        def path = "/hermes/groups/group/topics/topic/subscriptions/subscription/kafka_topics/kafkaTopic/offset/cluster/1"

        and:
        def command = commandFactory.setSubscriptionOffset(offsetData(42L, 1))
        command.backup(client)
        command.execute(client)

        when:
        command.rollback(client)

        then:
        wait.untilZookeeperPathNotExists(path)
    }

    def "should rollback subscription offset overwrite"() {
        given:
        def path = "/hermes/groups/group/topics/topic/subscriptions/subscription/kafka_topics/kafkaTopic/offset/cluster/2"

        and:
        def oldOffset = 21L
        def newOffset = 42L

        and:
        client.upsert(path, longToBytes(oldOffset))

        and:
        def command = commandFactory.setSubscriptionOffset(offsetData(newOffset, 2))
        command.backup(client)
        command.execute(client)

        when:
        command.rollback(client)

        then:
        assertions.zookeeperPathContains(path, oldOffset)
    }

    private static offsetData(long offset, int partition) {
        return new SetSubscriptionOffsetData(
            new TopicName("group", "topic"),
            "subscription",
            "cluster",
            new PartitionOffset(KafkaTopicName.valueOf("kafkaTopic"), offset, partition)
        )
    }

    private static longToBytes(long value) {
        return String.valueOf(value).getBytes(StandardCharsets.UTF_8)
    }

}
