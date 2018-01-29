package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import pl.allegro.tech.hermes.api.OAuthProvider
import pl.allegro.tech.hermes.test.IntegrationTest
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperAssertions

import static pl.allegro.tech.hermes.test.helper.builder.OAuthProviderBuilder.*


class UpdateOAuthProviderZookeeperCommandTest extends IntegrationTest {

    def client = zookeeperClient()
    def assertions = new ZookeeperAssertions(client.getCuratorFramework(), mapper)

    def "should update OAuth provider"() {
        given:
        def oldProvider = oAuthProvider("provider-update").withClientId("old-id").build()
        def newProvider = oAuthProvider("provider-update").withClientId("new-id").build()

        and:
        def path = "/hermes/oauth-providers/provider-update"

        and:
        createOAuthProvider(oldProvider)

        and:
        def command = commandFactory.updateOAuthProvider(newProvider)

        when:
        command.execute(client)

        then:
        assertions.zookeeperPathContains(path, newProvider)
    }

    def "should rollback OAuth provider update"() {
        given:
        def oldProvider = oAuthProvider("provider-rollback").withClientId("old-id").build()
        def newProvider = oAuthProvider("provider-rollback").withClientId("new-id").build()

        and:
        def path = "/hermes/oauth-providers/provider-rollback"

        and:
        createOAuthProvider(oldProvider)

        and:
        def command = commandFactory.updateOAuthProvider(newProvider)
        command.backup(client)
        command.execute(client)

        when:
        command.execute(client)

        then:
        assertions.zookeeperPathContains(path, oldProvider)
    }

    private def createOAuthProvider(OAuthProvider provider) {
        commandFactory.createOAuthProvider(provider).execute(client)
    }

}
