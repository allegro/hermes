package pl.allegro.tech.hermes.management.domain.workload.constraints

import com.fasterxml.jackson.databind.ObjectMapper
import pl.allegro.tech.hermes.api.Constraints
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.domain.workload.constraints.ConsumersWorkloadConstraints
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperWorkloadConstraintsRepository
import pl.allegro.tech.hermes.management.config.storage.DefaultZookeeperGroupRepositoryFactory
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperRepositoryManager
import pl.allegro.tech.hermes.management.utils.MultiZookeeperIntegrationTest

class WorkloadConstraintsServiceTest extends MultiZookeeperIntegrationTest {

    static WORKLOAD_CONSTRAINTS_PATH = '/hermes/consumers-workload-constraints'
    ZookeeperClientManager manager
    WorkloadConstraintsService service
    WorkloadConstraintsRepository repository
    ZookeeperRepositoryManager repositoryManager
    MultiDatacenterRepositoryCommandExecutor executor

    def objectMapper = new ObjectMapper()
    def paths = new ZookeeperPaths('/hermes')

    def setup() {
        manager = buildZookeeperClientManager()
        manager.start()
        assertZookeeperClientsConnected(manager.clients)
        manager.clients.each { client -> setupZookeeperPath(client, WORKLOAD_CONSTRAINTS_PATH) }
        repository = new ZookeeperWorkloadConstraintsRepository(manager.localClient.curatorFramework, objectMapper, paths)
        repositoryManager = new ZookeeperRepositoryManager(
                manager, new TestDatacenterNameProvider(DC_1_NAME), objectMapper,
                paths, new DefaultZookeeperGroupRepositoryFactory(), 180000)
        repositoryManager.start()
        executor = new MultiDatacenterRepositoryCommandExecutor(repositoryManager, false)
        service = new WorkloadConstraintsService(repository, executor)
    }

    def cleanup() {
        manager.stop()
    }

    def "should return constraints from local zookeeper cluster"() {
        given:
        setupLocalNode('group.topic', new Constraints(1))
        setupLocalNode('group.topic$sub', new Constraints(3))
        ensureCacheWasUpdated()

        when:
        def constraints = service.getConsumersWorkloadConstraints()

        then:
        constraints == new ConsumersWorkloadConstraints(
                [(TopicName.fromQualifiedName('group.topic')): new Constraints(1)],
                [(SubscriptionName.fromString('group.topic$sub')): new Constraints(3)]
        )
    }

    def "should create topic constraints in all zk clusters"() {
        when:
        service.createConstraints(TopicName.fromQualifiedName('group.topic'), new Constraints(1))

        then:
        assertNodesContains('group.topic', new Constraints(1))
    }

    def "should create subscription constraints in all zk clusters"() {
        when:
        service.createConstraints(SubscriptionName.fromString('group.topic$sub'), new Constraints(1))

        then:
        assertNodesContains('group.topic$sub', new Constraints(1))
    }

    def "should update topic constraints in all zk clusters"() {
        given:
        setupNodes('group.topic', new Constraints(1))

        when:
        service.updateConstraints(TopicName.fromQualifiedName('group.topic'), new Constraints(2))

        then:
        assertNodesContains('group.topic', new Constraints(2))
    }

    def "should update subscription constraints in all zk clusters"() {
        given:
        setupNodes('group.topic$sub', new Constraints(1))

        when:
        service.updateConstraints(SubscriptionName.fromString('group.topic$sub'), new Constraints(2))

        then:
        assertNodesContains('group.topic$sub', new Constraints(2))
    }

    def "should remove topic constraints in all zk clusters"() {
        given:
        setupNodes('group.topic', new Constraints(1))

        when:
        service.deleteConstraints(TopicName.fromQualifiedName('group.topic'))

        then:
        assertNodesDoesNotExist('group.topic')
    }

    def "should remove subscription constraints in all zk clusters"() {
        given:
        setupNodes('group.topic$sub', new Constraints(1))

        when:
        service.deleteConstraints(SubscriptionName.fromString('group.topic$sub'))

        then:
        assertNodesDoesNotExist('group.topic$sub')
    }

    def "should return true if topic constraints exist in local zk otherwise should return false"() {
        given:
        setupNodes('group.topic', new Constraints(1))
        ensureCacheWasUpdated()

        expect:
        service.constraintsExist(TopicName.fromQualifiedName('group.topic'))
        !service.constraintsExist(TopicName.fromQualifiedName('group.non-existent-topic'))
    }

    def "should return true if subscription constraints exist in local zk otherwise should return false"() {
        given:
        setupNodes('group.topic$sub', new Constraints(1))
        ensureCacheWasUpdated()

        expect:
        service.constraintsExist(SubscriptionName.fromString('group.topic$sub'))
        !service.constraintsExist(SubscriptionName.fromString('group.topic$non-existent-subscription'))
    }

    private def assertNodesContains(String path, Constraints expectedConstraints) {
        manager.clients.each {
            def data = it.curatorFramework.getData().forPath("$WORKLOAD_CONSTRAINTS_PATH/$path")
            def constraints = objectMapper.readValue(data, Constraints)
            assert constraints == expectedConstraints
        }
    }

    private def assertNodesDoesNotExist(String path) {
        manager.clients.each {
            assert it.curatorFramework.checkExists().forPath("$WORKLOAD_CONSTRAINTS_PATH/$path") == null
        }
    }

    private def setupLocalNode(String path, Constraints constraints) {
        def localClient = findClientByDc(manager.clients, DC_1_NAME)
        localClient.curatorFramework.create()
                .creatingParentsIfNeeded()
                .forPath("$WORKLOAD_CONSTRAINTS_PATH/$path", objectMapper.writeValueAsBytes(constraints))
    }

    private def setupNodes(String path, Constraints constraints) {
        manager.clients.each {
            it.curatorFramework.create()
                    .creatingParentsIfNeeded()
                    .forPath("$WORKLOAD_CONSTRAINTS_PATH/$path", objectMapper.writeValueAsBytes(constraints))
        }
    }

    private static ensureCacheWasUpdated() {
        Thread.sleep(200)
    }

    private static setupZookeeperPath(ZookeeperClient zookeeperClient, String path) {
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
