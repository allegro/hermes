package pl.allegro.tech.hermes.infrastructure.zookeeper

import pl.allegro.tech.hermes.api.OAuthProvider
import pl.allegro.tech.hermes.domain.oauth.OAuthProviderRepository
import pl.allegro.tech.hermes.test.IntegrationTest

class ZookeeperOAuthProviderRepositoryTest extends IntegrationTest {

    private OAuthProviderRepository repository = new ZookeeperOAuthProviderRepository(zookeeper(), mapper, paths)

    def setup() {
        if (zookeeper().checkExists().forPath(paths.oAuthProvidersPath())) {
            zookeeper().delete().deletingChildrenIfNeeded().forPath(paths.oAuthProvidersPath())
        }
    }

    def "should create oauth provider"() {
        def myProvider = getOAuthProvider("myProvider")
        when:
        repository.createOAuthProvider(myProvider)
        wait.untilOAuthProviderCreated(myProvider.name)

        then:
        repository.listOAuthProviderNames().contains(myProvider.name)
    }

    def "should update oauth provider"() {
        def myProvider = getOAuthProvider("myProvider")
        given:
        repository.createOAuthProvider(myProvider)
        wait.untilOAuthProviderCreated(myProvider.name)

        when:
        def updatedProvider = new OAuthProvider("myProvider", "http://example.com/token-updated", "client123",
                "pass123-updated", 1000, 2000, 1000, 1000)
        repository.updateOAuthProvider(updatedProvider)

        then:
        def actualProvider = repository.getOAuthProviderDetails(myProvider.name)
        actualProvider.tokenEndpoint == "http://example.com/token-updated"
        actualProvider.clientSecret == "pass123-updated"
    }

    def "should list all oauth providers"() {
        given:
        def myProvider = getOAuthProvider("myProvider")
        def myOtherProvider = getOAuthProvider("myOtherProvider")

        when:
        repository.createOAuthProvider(myProvider)
        wait.untilOAuthProviderCreated(myProvider.name)
        repository.createOAuthProvider(myOtherProvider)
        wait.untilOAuthProviderCreated(myOtherProvider.name)

        then:
        repository.listOAuthProviderNames().containsAll([myProvider.name, myOtherProvider.name])
        repository.listOAuthProviders().containsAll([myProvider, myOtherProvider])
    }

    def "should remove oauth provider"() {
        given:
        def myProvider = getOAuthProvider("myProvider")
        repository.createOAuthProvider(myProvider)
        wait.untilOAuthProviderCreated(myProvider.name)

        when:
        repository.removeOAuthProvider(myProvider.name)

        then:
        !repository.oAuthProviderExists(myProvider.name)
    }

    def "should get oauth provider created without socket timeout and set it default value"() {
        given:
        def myProvider = getOAuthProvider("myProvider")
        def provider = new OAuthProvider(myProvider.name, "http://example.com/token", "client123",
                "pass123", 1000, 2000, 1000, null)

        repository.createOAuthProvider(provider)
        wait.untilOAuthProviderCreated(myProvider.name)

        when:
        def fetchedProvider = repository.getOAuthProviderDetails(provider.name)

        then:
        fetchedProvider.socketTimeout == 0
    }

    private static getOAuthProvider(String name) {
        return new OAuthProvider(name, "http://example.com/token", "client123", "pass123", 1000, 2000, 1000, 1000)
    }
}
