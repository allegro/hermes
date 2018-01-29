package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.test.IntegrationTest
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperAssertions
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperWaiter


class UpdateGroupZookeeperCommandTest extends IntegrationTest {

    def client = zookeeperClient()
    def wait = new ZookeeperWaiter(client.getCuratorFramework())
    def assertions = new ZookeeperAssertions(client.getCuratorFramework(), mapper)

    def "should update group"() {
        given:
        def path = "/hermes/groups/group-name-update"

        and:
        def oldGroup = new Group("group-name-update", "old-support-team")
        def newGroup = new Group("group-name-update", "new-support-team")

        and:
        commandFactory.createGroup(oldGroup).execute(client)

        and:
        def command = commandFactory.updateGroup(newGroup)

        when:
        command.execute(client)

        then:
        assertions.zookeeperPathContains(path, newGroup)
    }

    def "should rollback group update"() {
        given:
        def path = "/hermes/groups/group-update-rollback"

        def oldGroup = new Group("group-update-rollback", "old-support-team")
        def newGroup = new Group("group-update-rollback", "new-support-team")

        and:
        def createGroupCommand = commandFactory.createGroup(oldGroup)
        createGroupCommand.execute(client)

        and:
        def updateGroupCommand = commandFactory.updateGroup(newGroup)
        updateGroupCommand.backup(client)
        updateGroupCommand.execute(client)

        when:
        updateGroupCommand.rollback(client)

        then:
        assertions.zookeeperPathContains(path, oldGroup)
    }
}
