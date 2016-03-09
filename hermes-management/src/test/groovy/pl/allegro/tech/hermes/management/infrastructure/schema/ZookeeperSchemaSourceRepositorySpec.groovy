package pl.allegro.tech.hermes.management.infrastructure.schema

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.test.TestingServer
import pl.allegro.tech.hermes.api.SchemaSource
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperWaiter
import spock.lang.Shared
import spock.lang.Specification

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class ZookeeperSchemaSourceRepositorySpec extends Specification {

    @Shared
    private TestingServer zookeeperServer = new TestingServer(6789)

    @Shared
    private CuratorFramework zookeeperClient

    @Shared
    private ZookeeperWaiter wait

    @Shared
    private ZookeeperSchemaSourceRepository repository

    void setupSpec() {
        zookeeperClient = CuratorFrameworkFactory.builder()
                .connectString(zookeeperServer.getConnectString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build()
        zookeeperClient.start()
        repository = new ZookeeperSchemaSourceRepository(zookeeperClient, new ZookeeperPaths("/test"))
        wait = new ZookeeperWaiter(zookeeperClient)
        wait.untilZookeeperClientStarted()
    }

    def "should save schema in zookeeper"() {
        given:
        def topic = topic("org.hermes.schema", "save").build()

        when:
        repository.save(SchemaSource.valueOf("schema"), topic)

        then:
        zookeeperClient.getData().forPath("/test/groups/org.hermes.schema/topics/save/schema") == "schema".bytes
    }

    def "should delete schema from zookeeper"() {
        given:
        def topic = topic("org.hermes.schema", "delete").build()
        zookeeperClient.create().creatingParentsIfNeeded().forPath("/test/groups/org.hermes.schema/topics/delete/schema", "toBeRemoved".bytes)

        when:
        repository.delete(topic)

        then:
        !zookeeperClient.checkExists().forPath("/test/groups/org.hermes.schema/topics/delete/schema")
    }

}
