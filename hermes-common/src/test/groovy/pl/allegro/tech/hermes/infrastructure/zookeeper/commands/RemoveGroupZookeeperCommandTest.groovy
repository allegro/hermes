package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.test.IntegrationTest
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperWaiter


class RemoveGroupZookeeperCommandTest extends IntegrationTest {

    def client = zookeeperClient()
    def wait = new ZookeeperWaiter(client)

    def "should remove group"() {
        given:
        def group = new Group("group-name-remove", "support-team")
        def createGroupOperation = commandFactory.createGroup(group)
        createGroupOperation.execute(client)

        def removeGroupOperation = commandFactory.removeGroup("group-name-remove")

        when:
        removeGroupOperation.execute(client)

        then:
        wait.untilZookeeperPathNotExists("/hermes/groups/group-name-remove")
    }

    def "should rollback group removal"() {
        given:
        def group = new Group("group-name-rollback", "support-team")
        def createGroupOperation = commandFactory.createGroup(group)
        createGroupOperation.execute(client)

        def removeGroupOperation = commandFactory.removeGroup("group-name-rollback")
        removeGroupOperation.backup(client)
        removeGroupOperation.execute(client)

        when:
        removeGroupOperation.rollback(client)

        then:
        wait.untilZookeeperPathIsCreated("/hermes/groups/group-name-rollback")
        getData(client, "/hermes/groups/group-name-rollback", Group.class) == group

        wait.untilZookeeperPathIsCreated("/hermes/groups/group-name-rollback/topics")
    }

}
