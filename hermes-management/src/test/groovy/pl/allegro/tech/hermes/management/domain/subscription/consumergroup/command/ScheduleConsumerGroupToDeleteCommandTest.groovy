package pl.allegro.tech.hermes.management.domain.subscription.consumergroup.command

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.management.config.storage.DefaultZookeeperGroupRepositoryFactory
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor
import pl.allegro.tech.hermes.management.domain.mode.ModeService
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupToDelete
import pl.allegro.tech.hermes.management.domain.subscription.consumergroup.ConsumerGroupToDeleteRepository
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperRepositoryManager
import pl.allegro.tech.hermes.management.utils.MultiZookeeperIntegrationTest
import spock.util.time.MutableClock

import java.time.Instant
import java.time.ZoneId

class ScheduleConsumerGroupToDeleteCommandTest extends MultiZookeeperIntegrationTest {
    ZookeeperClientManager zookeeperClientManager
    ZookeeperRepositoryManager repositoryManager
    Map<String, ConsumerGroupToDeleteRepository> consumerGroupToDeleteRepositoryMap
    MultiDatacenterRepositoryCommandExecutor executor
    MutableClock clock = new MutableClock(Instant.parse('2015-12-03T10:15:30.00Z'), ZoneId.of("UTC"))


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
        consumerGroupToDeleteRepositoryMap = repositoryManager.getRepositoriesByType(ConsumerGroupToDeleteRepository.class)
        executor = new MultiDatacenterRepositoryCommandExecutor(repositoryManager, true, new ModeService())
    }

    def cleanup() {
        zookeeperClientManager.stop()
    }

    def "Should schedule consumer group to delete"() {
        given:
        consumerGroupToDeleteRepositoryMap.values().forEach { assert it.getAllConsumerGroupsToDelete().size() == 0 }

        when:
        executor.execute(new ScheduleConsumerGroupToDeleteCommand(SubscriptionName.fromString("group.topic1\$subscription1"), clock.instant()))

        then:
        consumerGroupToDeleteRepositoryMap.forEach { datacenter, repository ->
            List<ConsumerGroupToDelete> toDelete = repository.getAllConsumerGroupsToDelete()
            assert toDelete.size() == 1
            with(toDelete.get(0)) {
                it.subscriptionName() == SubscriptionName.fromString("group.topic1\$subscription1")
                it.datacenter() == datacenter
                it.requestedAt() == clock.instant()
            }
        }
    }

    def "Should ignore scheduling consumer group to delete if it is already scheduled"() {
        given:
        executor.execute(new ScheduleConsumerGroupToDeleteCommand(SubscriptionName.fromString("group.topic1\$subscription1"), clock.instant()))

        when:
        executor.execute(new ScheduleConsumerGroupToDeleteCommand(SubscriptionName.fromString("group.topic1\$subscription1"), clock.instant().plusSeconds(1)))

        then:
        consumerGroupToDeleteRepositoryMap.forEach { datacenter, repository ->
            List<ConsumerGroupToDelete> consumerGroupsToDelete = repository.getAllConsumerGroupsToDelete()
            assert consumerGroupsToDelete.size() == 1
            with(consumerGroupsToDelete.get(0)) {
                it.subscriptionName() == SubscriptionName.fromString("group.topic1\$subscription1")
                it.datacenter() == datacenter
                it.requestedAt() == clock.instant()
            }
        }
    }
}
