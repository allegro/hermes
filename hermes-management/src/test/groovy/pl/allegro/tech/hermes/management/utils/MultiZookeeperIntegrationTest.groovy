package pl.allegro.tech.hermes.management.utils

import org.apache.curator.test.TestingServer
import pl.allegro.tech.hermes.management.infrastructure.dc.DcNameProvider
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient
import spock.lang.Specification

abstract class MultiZookeeperIntegrationTest extends Specification {

    static final int DC_1_ZOOKEEPER_PORT = 9500
    static final String DC_1_NAME = "dc1"
    static final int DC_2_ZOOKEEPER_PORT = 9501
    static final String DC_2_NAME = "dc2"

    static zookeeper1 = new TestingServer(DC_1_ZOOKEEPER_PORT, false)
    static zookeeper2 = new TestingServer(DC_2_ZOOKEEPER_PORT, false)

    def setupSpec() {
        zookeeper1.start()
        zookeeper2.start()
    }

    def cleanupSpec(){
        zookeeper1.stop()
        zookeeper2.stop()
    }

    static assertClientConnected(ZookeeperClient client) {
        return client.getCuratorFramework().getZookeeperClient().isConnected()
    }

    static assertClientDisconnected(ZookeeperClient client) {
        return !client.getCuratorFramework().getZookeeperClient().isConnected()
    }

    static class TestDcNameProvider implements DcNameProvider {

        private String dcName

        TestDcNameProvider(String dcName) {
            this.dcName = dcName
        }

        @Override
        String getDcName() {
            return dcName
        }
    }

}
