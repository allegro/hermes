package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import pl.allegro.tech.hermes.test.IntegrationTest
import pl.allegro.tech.hermes.test.helper.builder.GroupBuilder
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperWaiter

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic


class CreateTopicZookeeperCommandTest extends IntegrationTest {

    private static final String GROUP = "topicRepositoryGroup"

    def client = zookeeperClient()
    def wait = new ZookeeperWaiter(client.getCuratorFramework())

    def setup() {
        createGroupIfNotExists(GROUP)
    }

    def "should create topic"() {
        given:
        def topic = topic(GROUP, 'create-topic').build()

        and:
        def command = commandFactory.createTopic(topic)

        when:
        command.execute(client)

        then:
        wait.untilZookeeperPathIsCreated("/hermes/groups/$GROUP/topics/create-topic")
    }

    def "should rollback topic creation"() {
        given:
        def topic = topic(GROUP, 'rollback-topic').build()

        and:
        def command = commandFactory.createTopic(topic)
        command.backup(client)
        command.execute(client)
        wait.untilZookeeperPathIsCreated("/hermes/groups/$GROUP/topics/rollback-topic")

        when:
        command.rollback(client)

        then:
        wait.untilZookeeperPathNotExists("/hermes/groups/$GROUP/topics/rollback-topic")
    }

    private def createGroupIfNotExists(String name) {
        if(client.getCuratorFramework().checkExists().forPath("/hermes/groups/$name") == null) {
            def group = GroupBuilder.group(name).build()
            commandFactory.createGroup(group).execute(client)
        }
    }
}
