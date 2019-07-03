package pl.allegro.tech.hermes.management.domain.health

import com.fasterxml.jackson.databind.ObjectMapper
import pl.allegro.tech.hermes.management.config.storage.StorageClustersProperties
import pl.allegro.tech.hermes.management.config.storage.StorageProperties
import pl.allegro.tech.hermes.management.domain.mode.ModeService
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager
import pl.allegro.tech.hermes.management.utils.MultiZookeeperIntegrationTest

class HealthCheckTaskTest extends MultiZookeeperIntegrationTest {

    def healthCheckPath = '/hermes/storage-health/hostname_8080'
    def modeService = new ModeService()
    ZookeeperClientManager manager
    HealthCheckTask healthCheckTask

    def setup() {
        manager = buildZookeeperClientManager()
        manager.start()
        assertZookeeperClientsConnected(manager.clients)
        manager.clients.each { client -> setupZookeeperPath(client, healthCheckPath) }
        healthCheckTask = new HealthCheckTask(manager.clients, healthCheckPath, new ObjectMapper(), modeService)
    }

    def cleanup() {
        manager.stop()
    }

    def "should not change mode in case of successful health check"() {
        given:
        assert !modeService.readOnlyEnabled

        when:
        healthCheckTask.run()

        then:
        !modeService.readOnlyEnabled
    }

    def "should change mode to READ_ONLY in case of failed health check"() {
        given:
        assert !modeService.readOnlyEnabled

        and:
        zookeeper1.stop()

        when:
        healthCheckTask.run()

        then:
        modeService.readOnlyEnabled
    }

    def "should change mode to READ_ONLY in case of failed health check and set READ_WRITE back again in case of successful next connection"() {
        given:
        assert !modeService.readOnlyEnabled

        and:
        zookeeper1.stop()

        when:
        healthCheckTask.run()

        then:
        modeService.readOnlyEnabled

        and:
        zookeeper1.restart()

        and:
        healthCheckTask.run()

        and:
        !modeService.readOnlyEnabled
    }

    static buildZookeeperClientManager(String dc = "dc1") {
        def properties = new StorageClustersProperties(clusters: [
                new StorageProperties(connectionString: "localhost:$DC_1_ZOOKEEPER_PORT", datacenter: DC_1_NAME),
                new StorageProperties(connectionString: "localhost:$DC_2_ZOOKEEPER_PORT", datacenter: DC_2_NAME)
        ])
        new ZookeeperClientManager(properties, new TestDatacenterNameProvider(dc))
    }

    static findClientByDc(List<ZookeeperClient> clients, String dcName) {
        clients.find { it.datacenterName == dcName }
    }

    static setupZookeeperPath(ZookeeperClient zookeeperClient, String path) {
        def healthCheckPathExists = zookeeperClient.curatorFramework
                .checkExists()
                .forPath(path) != null
        if (!healthCheckPathExists) {
            zookeeperClient.curatorFramework
                    .create()
                    .creatingParentContainersIfNeeded()
                    .forPath(path)
        }
    }

    static assertZookeeperClientsConnected(List<ZookeeperClient> clients) {
        def dc1Client = findClientByDc(clients, DC_1_NAME)
        assert assertClientConnected(dc1Client)

        def dc2Client = findClientByDc(clients, DC_2_NAME)
        assert assertClientConnected(dc2Client)
    }
}
