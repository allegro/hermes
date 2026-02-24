package pl.allegro.tech.hermes.management.infrastructure.zookeeper

import pl.allegro.tech.hermes.infrastructure.dc.DefaultDatacenterNameProvider
import pl.allegro.tech.hermes.management.config.zookeeper.ZookeeperClustersProperties
import pl.allegro.tech.hermes.management.config.zookeeper.ZookeeperProperties
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
        localClient.datacenterName == localDcName

        cleanup:
        manager.stop()
    }

    def "should close clients after stop"() {
        given:
        def manager = buildZookeeperClientManager()
        manager.start()
        def clients = manager.clients
        assertZookeeperClientsConnected(clients)

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
        def clusterProperties = new ZookeeperProperties()
        clusterProperties.setConnectionString(topLevelConnectionString)
        clusterProperties.setDatacenter("dc")

        and:
        def manager = new ZookeeperClientManager([clusterProperties], new DefaultDatacenterNameProvider())

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
        def properties = new ZookeeperClustersProperties()
        properties.setClusters([
            buildStorageProperties("localhost:$DC_1_ZOOKEEPER_PORT", DC_1_NAME),
            buildStorageProperties("localhost:$DC_2_ZOOKEEPER_PORT", DC_2_NAME)
        ])
        return new ZookeeperClientManager(properties.getClusters(), new TestDatacenterNameProvider(dc))
    }

    static buildStorageProperties(String connectionString, String dcName) {
        def clusterProperties = new ZookeeperProperties()
        clusterProperties.setConnectionString(connectionString)
        clusterProperties.setDatacenter(dcName)
        return clusterProperties
    }
}
