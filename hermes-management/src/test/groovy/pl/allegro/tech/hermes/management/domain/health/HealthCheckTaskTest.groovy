package pl.allegro.tech.hermes.management.domain.health

import com.fasterxml.jackson.databind.ObjectMapper
import pl.allegro.tech.hermes.management.config.storage.StorageClustersProperties
import pl.allegro.tech.hermes.management.config.storage.StorageProperties
import pl.allegro.tech.hermes.management.domain.mode.ModeService
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager
import pl.allegro.tech.hermes.management.utils.MultiZookeeperIntegrationTest
import spock.lang.Ignore

import java.util.concurrent.TimeUnit

@Ignore
class HealthCheckTaskTest extends MultiZookeeperIntegrationTest {

    def healthCheckPath = '/hermes/management/health/hostname:8080'
    def modeService = new ModeService()

    @Ignore
    def "should not change mode on successful health check"() {
        given:
        def manager = buildZookeeperClientManager()
        manager.start()

        and:
        assertZookeeperClientsConnected(manager.clients)

        and:
        manager.clients.each { client -> setupZookeeperPath(client, healthCheckPath) }

        and:
        assert !modeService.readOnlyEnabled

        def healthCheckTask = new HealthCheckTask(manager.clients, healthCheckPath, new ObjectMapper(), modeService)

        when:
        healthCheckTask.run()

        then:
        !modeService.readOnlyEnabled

        cleanup:
        manager.stop()
    }

    @Ignore
    def "should change mode to READ_ONLY on failed health check"() {
        given:
        def manager = buildZookeeperClientManager()
        manager.start()

        and:
        assertZookeeperClientsConnected(manager.clients)

        and:
        manager.clients.each { client -> setupZookeeperPath(client, healthCheckPath) }

        and:
        assert !modeService.readOnlyEnabled

        and:
        zookeeper1.stop()

        def healthCheckTask = new HealthCheckTask(manager.clients, healthCheckPath, new ObjectMapper(), modeService)

        when:
        healthCheckTask.run()

        then:
        modeService.readOnlyEnabled

        cleanup:
        manager.stop()
    }

    @Ignore
    def "should change mode to READ_ONLY on failed health check and set READ_WRITE back again on successful next connection"() {
        given:
        def manager = buildZookeeperClientManager()
        manager.start()

        and:
        assertZookeeperClientsConnected(manager.clients)

        and:
        manager.clients.each { client -> setupZookeeperPath(client, healthCheckPath) }

        and:
        assert !modeService.readOnlyEnabled

        and:
        zookeeper1.stop()

        def healthCheckTask = new HealthCheckTask(manager.clients, healthCheckPath, new ObjectMapper(), modeService)

        when:
        healthCheckTask.run()

        then:
        modeService.readOnlyEnabled

        and:
        zookeeper1.start()
        manager.clients.each { client -> client.curatorFramework.blockUntilConnected(1, TimeUnit.SECONDS) }

        and:
        healthCheckTask.run()

        and:
        !modeService.readOnlyEnabled

        cleanup:
        manager.stop()
    }

    static buildZookeeperClientManager(String dc = "dc1") {
        def properties = new StorageClustersProperties(clusters: [
                new StorageProperties(connectionString: "localhost:$DC_1_ZOOKEEPER_PORT", dc: DC_1_NAME),
                new StorageProperties(connectionString: "localhost:$DC_2_ZOOKEEPER_PORT", dc: DC_2_NAME)
        ])
        new ZookeeperClientManager(properties, new TestDcNameProvider(dc))
    }

    static findClientByDc(List<ZookeeperClient> clients, String dcName) {
        clients.find { it.dcName == dcName }
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
        assert clients.size() == 2

        def dc1Client = findClientByDc(clients, DC_1_NAME)
        assert assertClientConnected(dc1Client)

        def dc2Client = findClientByDc(clients, DC_2_NAME)
        assert assertClientConnected(dc2Client)
    }
}
