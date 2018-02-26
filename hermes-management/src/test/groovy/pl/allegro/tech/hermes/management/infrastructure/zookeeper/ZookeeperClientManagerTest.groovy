package pl.allegro.tech.hermes.management.infrastructure.zookeeper

import pl.allegro.tech.hermes.management.config.storage.StorageClustersProperties
import pl.allegro.tech.hermes.management.config.storage.StorageProperties
import pl.allegro.tech.hermes.management.infrastructure.dc.DefaultDcNameProvider
import pl.allegro.tech.hermes.management.utils.MultiZookeeperIntegrationTest

class ZookeeperClientManagerTest extends MultiZookeeperIntegrationTest {

    def "should return all clients connected"() {
        given:
        def manager = buildZookeeperClientManager()
        manager.start()

        when:
        def clients = manager.clients

        then:
        clients.size() == 2

        and:
        def dc1Client = findClientByDc(clients, DC_1_NAME)
        assertClientConnected(dc1Client)

        and:
        def dc2Client = findClientByDc(clients, DC_2_NAME)
        assertClientConnected(dc2Client)

        cleanup:
        manager.stop()
    }

    def "should return local client"() {
        given:
        def localDcName = "dc2"

        and:
        def manager = buildZookeeperClientManager(localDcName)
        manager.start()

        when:
        def localClient = manager.localClient

        then:
        localClient.dcName == localDcName

        cleanup:
        manager.stop()
    }

    def "should close clients after stop"() {
        given:
        def manager = buildZookeeperClientManager()
        manager.start()
        def clients = manager.clients

        when:
        manager.stop()

        then:
        clients.size() == 2
        assertClientDisconnected(clients[0])
        assertClientDisconnected(clients[1])
    }

    def "should configure cluster from top-level properties if specific cluster properties are missing"() {
        given:
        def topLevelConnectionString = "localhost:$DC_1_ZOOKEEPER_PORT"

        and:
        def properties = new StorageClustersProperties()
        properties.setConnectionString(topLevelConnectionString)

        and:
        def manager = buildZookeeperClientManager(properties)

        when:
        manager.start()

        then:
        String connectionString = manager.getLocalClient().getCuratorFramework().getZookeeperClient()
            .getCurrentConnectionString()
        connectionString == topLevelConnectionString

        cleanup:
        manager.stop()
    }

    static buildZookeeperClientManager(String dc = "dc1") {
        def properties = new StorageClustersProperties()
        properties.setClusters([
            buildStorageProperties("localhost:$DC_1_ZOOKEEPER_PORT", DC_1_NAME),
            buildStorageProperties("localhost:$DC_2_ZOOKEEPER_PORT", DC_2_NAME)
        ])
        return new ZookeeperClientManager(properties, new TestDcNameProvider(dc))
    }

    static buildZookeeperClientManager(StorageClustersProperties properties) {
        return new ZookeeperClientManager(properties, new DefaultDcNameProvider())
    }

    static buildStorageProperties(String connectionString, String dcName) {
        def clusterProperties = new StorageProperties()
        clusterProperties.setConnectionString(connectionString)
        clusterProperties.setDc(dcName)
        return clusterProperties
    }

    static findClientByDc(List<ZookeeperClient> clients, String dcName) {
        return clients.find { it.dcName == dcName }
    }
}
