package pl.allegro.tech.hermes.consumers.subscription.id

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.api.ExistsBuilder
import org.apache.zookeeper.data.Stat
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.common.exception.InternalProcessingException
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import spock.lang.Shared
import spock.lang.Specification

class ZookeeperSubscriptionIdProviderTest extends Specification {

    @Shared
    def zookeeperPaths = new ZookeeperPaths("/hermes")

    def "should return id based on zookeeper czxid"() {
        given:
        def subscription = SubscriptionName.fromString('pl.allegro.tech.hermes$testSubscription')
        def curator = Stub(CuratorFramework) {
            def exists = Stub(ExistsBuilder) {
                forPath(zookeeperPaths.subscriptionPath(subscription.topicName, subscription.name)) >>
                        new Stat(123L, 456L, 100L, 200L, 1, 2, 3, 500L, 1, 0, 321L)
            }
            checkExists() >> exists
        }
        def provider = new ZookeeperSubscriptionIdProvider(curator, zookeeperPaths)

        when:
        def id = provider.getSubscriptionId(subscription)

        then:
        id.value == 123L
    }

    def "should throw exception on non-existing subscription"() {
        given:
        def subscription = SubscriptionName.fromString('pl.allegro.tech.hermes$nonExisting')
        def curator = Stub(CuratorFramework) {
            def exists = Stub(ExistsBuilder) {
                forPath(zookeeperPaths.subscriptionPath(subscription.topicName, subscription.name)) >> null
            }
            checkExists() >> exists
        }
        def provider = new ZookeeperSubscriptionIdProvider(curator, zookeeperPaths)

        when:
        provider.getSubscriptionId(subscription)

        then:
        def exception = thrown(IllegalStateException)
        exception.message == 'Cannot get czxid of subscription pl.allegro.tech.hermes$nonExisting as it doesn\'t exist'
    }

    def "should throw exception when unable to fetch node stat object"() {
        given:
        def subscription = SubscriptionName.fromString('pl.allegro.tech.hermes$failingSubscription')
        def curator = Stub(CuratorFramework) {
            def exists = Stub(ExistsBuilder) {
                forPath(zookeeperPaths.subscriptionPath(subscription.topicName, subscription.name)) >>
                        { throw new RuntimeException("An error occurred") }
            }
            checkExists() >> exists
        }
        def provider = new ZookeeperSubscriptionIdProvider(curator, zookeeperPaths)

        when:
        provider.getSubscriptionId(subscription)

        then:
        def exception = thrown(InternalProcessingException)
        exception.message == 'Could not check existence of subscription pl.allegro.tech.hermes$failingSubscription node'
    }
}
