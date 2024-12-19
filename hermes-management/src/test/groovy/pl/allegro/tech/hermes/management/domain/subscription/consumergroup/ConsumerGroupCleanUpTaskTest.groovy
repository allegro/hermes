package pl.allegro.tech.hermes.management.domain.subscription.consumergroup

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.management.config.storage.DefaultZookeeperGroupRepositoryFactory
import pl.allegro.tech.hermes.management.config.subscription.consumergroup.ConsumerGroupCleanUpProperties
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor
import pl.allegro.tech.hermes.management.domain.mode.ModeService
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.command.ScheduleConsumerGroupToDeleteCommand
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService
import pl.allegro.tech.hermes.management.infrastructure.leader.ManagementLeadership
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperRepositoryManager
import pl.allegro.tech.hermes.management.utils.MultiZookeeperIntegrationTest
import spock.lang.Subject
import spock.util.time.MutableClock

import java.time.Instant
import java.time.ZoneId

class ConsumerGroupCleanUpTaskTest extends MultiZookeeperIntegrationTest {
    ZookeeperClientManager zookeeperClientManager
    ZookeeperRepositoryManager repositoryManager
    MultiDatacenterRepositoryCommandExecutor executor
    Map<String, ConsumerGroupToDeleteRepository> consumerGroupToDeleteRepositoryMap

    MultiDCAwareService multiDCAwareService = Mock(MultiDCAwareService)
    SubscriptionService subscriptionService = Mock(SubscriptionService)
    ManagementLeadership managementLeadership = Mock(ManagementLeadership)
    ConsumerGroupCleanUpProperties consumerGroupCleanUpProperties = new ConsumerGroupCleanUpProperties()
    MutableClock clock = new MutableClock(Instant.parse('2015-12-03T10:15:30.00Z'), ZoneId.of("UTC"))

    @Subject
    ConsumerGroupCleanUpTask consumerGroupCleanUpTask

    def setup() {
        zookeeperClientManager = buildZookeeperClientManager()
        zookeeperClientManager.start()
        assertZookeeperClientsConnected(zookeeperClientManager.clients)

        repositoryManager = new ZookeeperRepositoryManager(zookeeperClientManager,
                new TestDatacenterNameProvider(DC_1_NAME),
                new ObjectMapper().registerModule(new JavaTimeModule()),
                new ZookeeperPaths('/hermes'),
                new DefaultZookeeperGroupRepositoryFactory())
        repositoryManager.start()
        executor = new MultiDatacenterRepositoryCommandExecutor(repositoryManager, true, new ModeService())
        consumerGroupToDeleteRepositoryMap = repositoryManager.getRepositoriesByType(ConsumerGroupToDeleteRepository.class)

        consumerGroupCleanUpTask = new ConsumerGroupCleanUpTask(multiDCAwareService,
                consumerGroupToDeleteRepositoryMap,
                subscriptionService,
                consumerGroupCleanUpProperties,
                managementLeadership,
                clock)
    }

    def cleanup() {
        zookeeperClientManager.stop()
    }

    def "should remove all consumer groups which are scheduled to delete"() {
        given:
        executor.execute(new ScheduleConsumerGroupToDeleteCommand(SubscriptionName.fromString("group.topic1\$subscription1"), clock.instant()))
        executor.execute(new ScheduleConsumerGroupToDeleteCommand(SubscriptionName.fromString("group.topic1\$subscription2"), clock.instant()))
        executor.execute(new ScheduleConsumerGroupToDeleteCommand(SubscriptionName.fromString("group.topic2\$subscription1"), clock.instant()))

        and:
        clock + consumerGroupCleanUpProperties.getInitialDelay()

        when:
        consumerGroupCleanUpTask.run()

        then:
        for (String datacenter : zookeeperClientManager.getClients().collect { it.datacenterName }) {
            1 * multiDCAwareService.deleteConsumerGroupForDatacenter(SubscriptionName.fromString("group.topic1\$subscription1"), datacenter)
            1 * multiDCAwareService.deleteConsumerGroupForDatacenter(SubscriptionName.fromString("group.topic1\$subscription2"), datacenter)
            1 * multiDCAwareService.deleteConsumerGroupForDatacenter(SubscriptionName.fromString("group.topic2\$subscription1"), datacenter)
        }

        and:
        2 * subscriptionService.subscriptionExists(SubscriptionName.fromString("group.topic1\$subscription1")) >> false
        2 * subscriptionService.subscriptionExists(SubscriptionName.fromString("group.topic1\$subscription2")) >> false
        2 * subscriptionService.subscriptionExists(SubscriptionName.fromString("group.topic2\$subscription1")) >> false

        and:
        managementLeadership.isLeader() >> true

        and:
        consumerGroupToDeleteRepositoryMap.values().every { it.getAllConsumerGroupsToDelete().size() == 0 }
    }

    def "should skip removal of consumer group - subscription exist"() {
        given:
        executor.execute(new ScheduleConsumerGroupToDeleteCommand(SubscriptionName.fromString("group.topic1\$subscription1"), clock.instant()))

        and:
        clock + consumerGroupCleanUpProperties.getInitialDelay()

        when:
        consumerGroupCleanUpTask.run()

        then:
        for (String datacenter : zookeeperClientManager.getClients().collect { it.datacenterName }) {
            0 * multiDCAwareService.deleteConsumerGroupForDatacenter(SubscriptionName.fromString("group.topic1\$subscription1"), datacenter)
        }

        and:
        2 * subscriptionService.subscriptionExists(SubscriptionName.fromString("group.topic1\$subscription1")) >> true

        and:
        managementLeadership.isLeader() >> true

        and:
        consumerGroupToDeleteRepositoryMap.values().every { it.getAllConsumerGroupsToDelete().size() == 0 }
    }

    def "should skip removal of consumer group - initial delay not met"() {
        given:
        executor.execute(new ScheduleConsumerGroupToDeleteCommand(SubscriptionName.fromString("group.topic1\$subscription1"), clock.instant()))

        when:
        consumerGroupCleanUpTask.run()

        then:
        for (String datacenter : zookeeperClientManager.getClients().collect { it.datacenterName }) {
            0 * multiDCAwareService.deleteConsumerGroupForDatacenter(SubscriptionName.fromString("group.topic1\$subscription1"), datacenter)
        }

        and:
        0 * subscriptionService.subscriptionExists(SubscriptionName.fromString("group.topic1\$subscription1"))

        and:
        managementLeadership.isLeader() >> true

        and:
        consumerGroupToDeleteRepositoryMap.values().every { it.getAllConsumerGroupsToDelete().size() == 1 }
    }

    def "should skip removal of consumer group - task timeout not met"() {
        given:
        executor.execute(new ScheduleConsumerGroupToDeleteCommand(SubscriptionName.fromString("group.topic1\$subscription1"), clock.instant()))

        and:
        clock + consumerGroupCleanUpProperties.getTimeout().plus(consumerGroupCleanUpProperties.getTimeout()).plusSeconds(1)

        when:
        consumerGroupCleanUpTask.run()

        then:
        for (String datacenter : zookeeperClientManager.getClients().collect { it.datacenterName }) {
            0 * multiDCAwareService.deleteConsumerGroupForDatacenter(SubscriptionName.fromString("group.topic1\$subscription1"), datacenter)
        }

        and:
        2 * subscriptionService.subscriptionExists(SubscriptionName.fromString("group.topic1\$subscription1")) >> false

        and:
        managementLeadership.isLeader() >> true

        and:
        consumerGroupToDeleteRepositoryMap.values().every { it.getAllConsumerGroupsToDelete().size() == 0 }
    }

    def "should skip removal of consumer group - task timeout not met, without task removal"() {
        given:
        executor.execute(new ScheduleConsumerGroupToDeleteCommand(SubscriptionName.fromString("group.topic1\$subscription1"), clock.instant()))

        and:
        ConsumerGroupCleanUpProperties cleanUpProperties = new ConsumerGroupCleanUpProperties()
        cleanUpProperties.removeTasksAfterTimeout = false;
        ConsumerGroupCleanUpTask consumerGroupCleanUpTaskWithoutExpiredTaskRemoval = new ConsumerGroupCleanUpTask(multiDCAwareService,
                consumerGroupToDeleteRepositoryMap,
                subscriptionService,
                cleanUpProperties,
                managementLeadership,
                clock)

        and:
        clock + cleanUpProperties.getTimeout().plus(cleanUpProperties.getTimeout()).plusSeconds(1)

        when:
        consumerGroupCleanUpTaskWithoutExpiredTaskRemoval.run()

        then:
        for (String datacenter : zookeeperClientManager.getClients().collect { it.datacenterName }) {
            0 * multiDCAwareService.deleteConsumerGroupForDatacenter(SubscriptionName.fromString("group.topic1\$subscription1"), datacenter)
        }

        and:
        2 * subscriptionService.subscriptionExists(SubscriptionName.fromString("group.topic1\$subscription1")) >> false

        and:
        managementLeadership.isLeader() >> true

        and:
        consumerGroupToDeleteRepositoryMap.values().every { it.getAllConsumerGroupsToDelete().size() == 1 }
    }

    def "Should skip removal of consumer group - not leader"() {
        given:
        executor.execute(new ScheduleConsumerGroupToDeleteCommand(SubscriptionName.fromString("group.topic1\$subscription1"), clock.instant()))

        and:
        clock + consumerGroupCleanUpProperties.getInitialDelay()

        when:
        consumerGroupCleanUpTask.run()

        then:
        for (String datacenter : zookeeperClientManager.getClients().collect { it.datacenterName }) {
            0 * multiDCAwareService.deleteConsumerGroupForDatacenter(SubscriptionName.fromString("group.topic1\$subscription1"), datacenter)
        }

        and:
        0 * subscriptionService.subscriptionExists(SubscriptionName.fromString("group.topic1\$subscription1"))

        and:
        managementLeadership.isLeader() >> false

        and:
        consumerGroupToDeleteRepositoryMap.values().every { it.getAllConsumerGroupsToDelete().size() == 1 }
    }

    def "should properly handle exceptions"() {
        given:
        executor.execute(new ScheduleConsumerGroupToDeleteCommand(SubscriptionName.fromString("group.topic1\$subscription1"), clock.instant()))
        executor.execute(new ScheduleConsumerGroupToDeleteCommand(SubscriptionName.fromString("group.topic1\$subscription2"), clock.instant()))

        and:
        clock + consumerGroupCleanUpProperties.getInitialDelay()

        when:
        consumerGroupCleanUpTask.run()

        then:
        for (String datacenter : zookeeperClientManager.getClients().collect { it.datacenterName }) {
            1 * multiDCAwareService.deleteConsumerGroupForDatacenter(SubscriptionName.fromString("group.topic1\$subscription1"), datacenter) >> { throw new RuntimeException() }
            1 * multiDCAwareService.deleteConsumerGroupForDatacenter(SubscriptionName.fromString("group.topic1\$subscription2"), datacenter)
        }

        and:
        2 * subscriptionService.subscriptionExists(SubscriptionName.fromString("group.topic1\$subscription1")) >> false
        2 * subscriptionService.subscriptionExists(SubscriptionName.fromString("group.topic1\$subscription2")) >> false

        and:
        managementLeadership.isLeader() >> true

        and:
        consumerGroupToDeleteRepositoryMap.forEach { datacenter, repository ->
            List<ConsumerGroupToDelete> consumerGroupsToDelete = repository.getAllConsumerGroupsToDelete()
            assert consumerGroupsToDelete.size() == 1
            with(consumerGroupsToDelete.get(0)) {
                it.subscriptionName() == SubscriptionName.fromString("group.topic1\$subscription1")
                it.datacenter() == datacenter
                it.requestedAt() == clock.instant() - consumerGroupCleanUpProperties.getInitialDelay()
            }
        }

        when:
        consumerGroupCleanUpTask.run()

        then:
        for (String datacenter : zookeeperClientManager.getClients().collect { it.datacenterName }) {
            1 * multiDCAwareService.deleteConsumerGroupForDatacenter(SubscriptionName.fromString("group.topic1\$subscription1"), datacenter)
        }

        and:
        2 * subscriptionService.subscriptionExists(SubscriptionName.fromString("group.topic1\$subscription1")) >> false

        and:
        managementLeadership.isLeader() >> true

        and:
        consumerGroupToDeleteRepositoryMap.values().every { it.getAllConsumerGroupsToDelete().size() == 0 }
    }
}
