package pl.allegro.tech.hermes.management.utils

import org.apache.curator.test.TestingServer
import pl.allegro.tech.hermes.management.infrastructure.dc.DatacenterNameProvider
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient
import pl.allegro.tech.hermes.test.helper.util.Ports
import spock.lang.Specification

abstract class MultiZookeeperIntegrationTest extends Specification {

    static final int DC_1_ZOOKEEPER_PORT = Ports.nextAvailable()
    static final String DC_1_NAME = "dc1"
    static final int DC_2_ZOOKEEPER_PORT = Ports.nextAvailable()
    static final String DC_2_NAME = "dc2"

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

    static assertClientConnected(ZookeeperClient client) {
        return client.getCuratorFramework().getZookeeperClient().isConnected()
    }

    static assertClientDisconnected(ZookeeperClient client) {
        return !client.getCuratorFramework().getZookeeperClient().isConnected()
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
