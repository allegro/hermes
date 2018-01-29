package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient
import pl.allegro.tech.hermes.test.IntegrationTest
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperAssertions

import static pl.allegro.tech.hermes.test.helper.builder.OAuthProviderBuilder.oAuthProvider


class RemoveOAuthProviderZookeeperCommandTest extends IntegrationTest {

    private ZookeeperClient client = zookeeperClient()
    private ZookeeperAssertions assertions = new ZookeeperAssertions(client.getCuratorFramework(), mapper)

    def "should remove OAuth provider"() {
        given:
        def provider = oAuthProvider("provider-remove").build()
        def path = "/hermes/oauth-providers/provider-remove"

        and:
        def creationCommand = commandFactory.createOAuthProvider(provider)
        creationCommand.execute(client)
        wait.untilZookeeperPathIsCreated(path)

        and:
        def removalCommand = commandFactory.removeOAuthProvider("provider-remove")

        when:
        removalCommand.execute(client)

        then:
        wait.untilZookeeperPathNotExists(path)
    }

    def "should rollback OAuth provider removal"() {
        given:
        def provider = oAuthProvider("provider-remove-rollback").build()
        def path = "/hermes/oauth-providers/provider-remove-rollback"

        and:
        def creationCommand = commandFactory.createOAuthProvider(provider)
        creationCommand.execute(client)
        wait.untilZookeeperPathIsCreated(path)

        and:
        def removalCommand = commandFactory.removeOAuthProvider("provider-remove-rollback")
        removalCommand.backup(client)
        removalCommand.execute(client)
        wait.untilZookeeperPathNotExists(path)

        when:
        removalCommand.rollback(client)

        then:
        wait.untilZookeeperPathIsCreated(path)
        assertions.zookeeperPathContains(path, provider)
    }

}
