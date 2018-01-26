package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.test.IntegrationTest
import pl.allegro.tech.hermes.test.helper.builder.GroupBuilder
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperWaiter

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.*


class RemoveTopicZookeeperCommandTest extends IntegrationTest {

    private static final String GROUP = "group"
    private static final String TOPIC = "topic"

    def client = zookeeperClient()
    def wait = new ZookeeperWaiter(client.getCuratorFramework())

    def topic = topic(GROUP, TOPIC).build()

    def setup() {
        if(client.getCuratorFramework().checkExists().forPath("/hermes/groups/$GROUP") == null) {
            commandFactory.createGroup(GroupBuilder.group(GROUP).build()).execute(client)

            if(client.getCuratorFramework().checkExists().forPath("/hermes/groups/$GROUP/topics/$TOPIC") == null) {
                commandFactory.createTopic(topic).execute(client)
            }
        }
    }

    def "should remove topic"() {
        given:
        def command = commandFactory.removeTopic(new TopicName(GROUP, TOPIC))

        when:
        command.execute(client)

        then:
        wait.untilZookeeperPathNotExists("/hermes/groups/$GROUP/topics/$TOPIC")
    }

    def "should rollback topic removal"() {
        given:
        def topicPath = "/hermes/groups/$GROUP/topics/$TOPIC"

        and:
        def command = commandFactory.removeTopic(new TopicName(GROUP, TOPIC))
        command.backup(client)
        command.execute(client)
        wait.untilZookeeperPathNotExists(topicPath)

        when:
        command.rollback(client)

        then:
        wait.untilZookeeperPathIsCreated(topicPath)
        getData(client, topicPath, Topic.class) == topic

        wait.untilZookeeperPathIsCreated("$topicPath/subscriptions")
    }

}
