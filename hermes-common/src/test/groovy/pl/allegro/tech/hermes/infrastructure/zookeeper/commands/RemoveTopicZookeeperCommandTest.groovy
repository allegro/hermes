package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.test.IntegrationTest
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperAssertions
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperWaiter

class RemoveTopicZookeeperCommandTest extends IntegrationTest {

    def client = zookeeperClient()
    def wait = new ZookeeperWaiter(client.getCuratorFramework())
    def assertions = new ZookeeperAssertions(client.getCuratorFramework(), mapper)

    Topic topic

    def topicPath = "/hermes/groups/group/topics/topic-remove"

    def setup() {
        createGroupIfNotExists("group")
        topic = createTopicIfNotExists("topic-remove", "group")
    }

    def "should remove topic"() {
        given:

        def command = commandFactory.removeTopic(new TopicName("group", "topic-remove"))

        when:
        command.execute(client)

        then:
        wait.untilZookeeperPathNotExists(topicPath)
    }

    def "should rollback topic removal"() {
        given:
        def command = commandFactory.removeTopic(new TopicName("group", "topic-remove"))
        command.backup(client)
        command.execute(client)
        wait.untilZookeeperPathNotExists(topicPath)

        when:
        command.rollback(client)

        then:
        wait.untilZookeeperPathIsCreated(topicPath)
        assertions.zookeeperPathContains(topicPath, topic)

        wait.untilZookeeperPathIsCreated("$topicPath/subscriptions")
    }

}
