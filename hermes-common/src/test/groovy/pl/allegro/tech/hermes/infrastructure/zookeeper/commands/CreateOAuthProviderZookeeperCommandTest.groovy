package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import pl.allegro.tech.hermes.test.IntegrationTest
import pl.allegro.tech.hermes.test.helper.builder.OAuthProviderBuilder
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperAssertions
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperWaiter

class CreateOAuthProviderZookeeperCommandTest extends IntegrationTest {

    def client = zookeeperClient()
    def wait = new ZookeeperWaiter(client.getCuratorFramework())
    def assertions = new ZookeeperAssertions(client.getCuratorFramework(), mapper)

    def "should create OAuth provider"() {
        given:
        def provider = OAuthProviderBuilder.oAuthProvider("provider-create").build()
        def command = commandFactory.createOAuthProvider(provider)

        when:
        command.execute(client)

        then:
        def path = "/hermes/oauth-providers/provider-create"
        wait.untilZookeeperPathIsCreated(path)
        assertions.zookeeperPathContains(path, provider)
    }

    def "should rollback OAuth provider creation"() {
        given:
        def provider = OAuthProviderBuilder.oAuthProvider("provider-rollback").build()
        String path = "/hermes/oauth-providers/provider-rollback"

        and:
        def command = commandFactory.createOAuthProvider(provider)
        command.backup(client)
        command.execute(client)
        wait.untilZookeeperPathIsCreated(path)

        when:
        command.rollback(client)

        then:
        wait.untilZookeeperPathNotExists(path)
    }
}
