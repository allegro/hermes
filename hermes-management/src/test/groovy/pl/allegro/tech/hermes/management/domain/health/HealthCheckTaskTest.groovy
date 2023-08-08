package pl.allegro.tech.hermes.management.domain.health

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import pl.allegro.tech.hermes.management.domain.mode.ModeService
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager
import pl.allegro.tech.hermes.management.utils.MultiZookeeperIntegrationTest

class HealthCheckTaskTest extends MultiZookeeperIntegrationTest {

    def healthCheckPath = '/hermes/storage-health/hostname_8080'
    def modeService = new ModeService()
    ZookeeperClientManager manager
    HealthCheckTask healthCheckTask
    def meterRegistry = new SimpleMeterRegistry()
    Counter successfulCounter
    Counter failedCounter

    def setup() {
        manager = buildZookeeperClientManager()
        manager.start()
        assertZookeeperClientsConnected(manager.clients)
        manager.clients.each { client -> setupZookeeperPath(client, healthCheckPath) }
        healthCheckTask = new HealthCheckTask(manager.clients, healthCheckPath, new ObjectMapper().registerModule(new JavaTimeModule()), modeService, meterRegistry)
        successfulCounter = meterRegistry.counter('storage-health-check.successful')
        failedCounter = meterRegistry.counter('storage-health-check.failed')
        modeService.mode = ModeService.ManagementMode.READ_WRITE
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

        and:
        successfulCounter.count() == 2
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

        and:
        successfulCounter.count() == 1
        failedCounter.count() == 1
    }

    def "should change mode to READ_ONLY in case of failed health check and set READ_WRITE back again in case of successful next connection"() {
        given:
        assert !modeService.readOnlyEnabled

        when:
        zookeeper1.stop()
        healthCheckTask.run()

        then:
        modeService.readOnlyEnabled

        and:
        successfulCounter.count() == 1
        failedCounter.count() == 1

        when:
        zookeeper1.restart()
        healthCheckTask.run()

        then:
        !modeService.readOnlyEnabled

        and:
        successfulCounter.count() == 3
    }

    def "should not change status to READ_WRITE if READ_ONLY is set by admin"() {
        given:
        modeService.setModeByAdmin(ModeService.ManagementMode.READ_ONLY_ADMIN)

        when:
        healthCheckTask.run()

        then:
        modeService.readOnlyEnabled

        and:
        successfulCounter.count() == 2
    }

    def "should change status from READ_ONLY_ADMIN to READ_WRITE only by admin"() {
        given:
        modeService.setModeByAdmin(ModeService.ManagementMode.READ_ONLY_ADMIN)

        when:
        healthCheckTask.run()

        then:
        modeService.readOnlyEnabled

        when:
        modeService.setModeByAdmin(ModeService.ManagementMode.READ_WRITE)

        then:
        !modeService.readOnlyEnabled
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
}
