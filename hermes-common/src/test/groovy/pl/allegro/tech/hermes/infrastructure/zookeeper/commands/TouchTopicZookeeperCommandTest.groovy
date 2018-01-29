package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.test.IntegrationTest


class TouchTopicZookeeperCommandTest extends IntegrationTest {

    def client = zookeeperClient()

    def "should touch topic"() {
        given:
        createGroupIfNotExists("group")
        createTopicIfNotExists("topic", "group")

        and:
        def versionBeforeTouch = getVersion()

        and:
        def command = commandFactory.touchTopic(new TopicName("group", "topic"))

        when:
        command.execute(client)

        then:
        def versionAfterTouch = getVersion()
        versionAfterTouch > versionBeforeTouch
    }

    def getVersion() {
        return client.getCuratorFramework().checkExists().forPath("/hermes/groups/group/topics/topic").version
    }
}
