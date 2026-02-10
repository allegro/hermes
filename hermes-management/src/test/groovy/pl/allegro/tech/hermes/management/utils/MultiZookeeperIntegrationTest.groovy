package pl.allegro.tech.hermes.management.utils

import java.time.Duration
import org.apache.curator.test.TestingServer
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider
import pl.allegro.tech.hermes.management.config.zookeeper.ZookeeperProperties
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

abstract class MultiZookeeperIntegrationTest extends Specification {

    static final int DC_1_ZOOKEEPER_PORT = Ports.nextAvailable()
    static final String DC_1_NAME = "dc1"
    static final int DC_2_ZOOKEEPER_PORT = Ports.nextAvailable()
    static final String DC_2_NAME = "dc2"

    def conditions = new PollingConditions(timeout: 120)

    TestingServer zookeeper1
    TestingServer zookeeper2

    def setup() {
        zookeeper1 = new TestingServer(DC_1_ZOOKEEPER_PORT, false)
        zookeeper2 = new TestingServer(DC_2_ZOOKEEPER_PORT, false)
        zookeeper1.start()
        zookeeper2.start()
    }

    def cleanup() {
        zookeeper1.stop()
        zookeeper2.stop()
    }

    void assertClientConnected(ZookeeperClient client) {
        conditions.eventually {
            assert client.getCuratorFramework().getZookeeperClient().isConnected()
        }
    }

    void assertClientDisconnected(ZookeeperClient client) {
        conditions.eventually {
            assert !client.getCuratorFramework().getZookeeperClient().isConnected()
        }
    }

    void assertZookeeperClientsConnected(List<ZookeeperClient> clients) {
        def dc1Client = findClientByDc(clients, DC_1_NAME)
        assertClientConnected(dc1Client)

        def dc2Client = findClientByDc(clients, DC_2_NAME)
        assertClientConnected(dc2Client)
    }

    static buildZookeeperClientManager(String dc = "dc1") {
        def clusters = [
                buildZookeeperProperties("localhost:$DC_1_ZOOKEEPER_PORT", DC_1_NAME),
                buildZookeeperProperties("localhost:$DC_2_ZOOKEEPER_PORT", DC_2_NAME)
        ]
        new ZookeeperClientManager(clusters, new TestDatacenterNameProvider(dc))
    }

    static buildZookeeperProperties(String connectionString, String datacenter) {
        def properties = new ZookeeperProperties()
        properties.setConnectionString(connectionString)
        properties.setDatacenter(datacenter)
        properties.setBaseSleepTime(Duration.ofMillis(100))
        properties.setMaxRetries(3)
        properties.setConnectionTimeout(Duration.ofSeconds(2))
        properties.setSessionTimeout(Duration.ofSeconds(5))
        return properties
    }

    static findClientByDc(List<ZookeeperClient> clients, String dcName) {
        clients.find { it.datacenterName == dcName }
    }

    static class TestDatacenterNameProvider implements DatacenterNameProvider {

        private String dcName

        TestDatacenterNameProvider(String dcName) {
            this.dcName = dcName
        }

        @Override
        String getDatacenterName() {
            return dcName
        }
    }

}
