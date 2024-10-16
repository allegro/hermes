package pl.allegro.tech.hermes.management.domain.workload.constraints

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import pl.allegro.tech.hermes.api.Constraints
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.common.exception.InternalProcessingException
import pl.allegro.tech.hermes.domain.workload.constraints.ConsumersWorkloadConstraints
import pl.allegro.tech.hermes.domain.workload.constraints.SubscriptionConstraintsAlreadyExistException
import pl.allegro.tech.hermes.domain.workload.constraints.SubscriptionConstraintsDoNotExistException
import pl.allegro.tech.hermes.domain.workload.constraints.TopicConstraintsAlreadyExistException
import pl.allegro.tech.hermes.domain.workload.constraints.TopicConstraintsDoNotExistException
import pl.allegro.tech.hermes.domain.workload.constraints.WorkloadConstraintsRepository
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperWorkloadConstraintsRepository
import pl.allegro.tech.hermes.management.config.storage.DefaultZookeeperGroupRepositoryFactory
import pl.allegro.tech.hermes.management.domain.auth.TestRequestUser
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor
import pl.allegro.tech.hermes.management.domain.mode.ModeService
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperRepositoryManager
import pl.allegro.tech.hermes.management.utils.MultiZookeeperIntegrationTest

class WorkloadConstraintsServiceTest extends MultiZookeeperIntegrationTest {

    static WORKLOAD_CONSTRAINTS_PATH = '/hermes/consumers-workload-constraints'
    static USER = new TestRequestUser("username", false)
    ZookeeperClientManager manager
    WorkloadConstraintsService service
    WorkloadConstraintsRepository repository
    ZookeeperRepositoryManager repositoryManager
    ModeService modeService
    MultiDatacenterRepositoryCommandExecutor executor

    def objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
    def paths = new ZookeeperPaths('/hermes')

    def setup() {
        manager = buildZookeeperClientManager()
        manager.start()
        assertZookeeperClientsConnected(manager.clients)
        manager.clients.each { client -> setupZookeeperPath(client, WORKLOAD_CONSTRAINTS_PATH) }
        repository = new ZookeeperWorkloadConstraintsRepository(manager.localClient.curatorFramework, objectMapper, paths)
        repositoryManager = new ZookeeperRepositoryManager(
                manager, new TestDatacenterNameProvider(DC_1_NAME), objectMapper,
                paths, new DefaultZookeeperGroupRepositoryFactory())
        repositoryManager.start()
        modeService = new ModeService()
        executor = new MultiDatacenterRepositoryCommandExecutor(repositoryManager, true, modeService)
        service = new WorkloadConstraintsService(repository, executor)
    }

    def cleanup() {
        manager.stop()
    }

    def "should return constraints from local zookeeper cluster"() {
        given:
        setupLocalNode('group.topic', new Constraints(1, null))
        setupLocalNode('group.topic$sub', new Constraints(3, null))
        ensureCacheWasUpdated()

        when:
        def constraints = service.getConsumersWorkloadConstraints()

        then:
        constraints == new ConsumersWorkloadConstraints(
                [(TopicName.fromQualifiedName('group.topic')): new Constraints(1, null)],
                [(SubscriptionName.fromString('group.topic$sub')): new Constraints(3, null)]
        )
    }

    def "should create topic constraints in all zk clusters"() {
        when:
        service.createConstraints(TopicName.fromQualifiedName('group.topic'), new Constraints(1, null), USER)

        then:
        assertNodesContains('group.topic', new Constraints(1, null))
    }

    def "should create subscription constraints in all zk clusters"() {
        when:
        service.createConstraints(SubscriptionName.fromString('group.topic$sub'), new Constraints(1, null), USER)

        then:
        assertNodesContains('group.topic$sub', new Constraints(1, null))
    }

    def "nodes should remain unchanged in case of failure while creating topic constraints"() {
        given:
        setupNode(DC_2_NAME, 'group.topic', new Constraints(2, null))
        ensureCacheWasUpdated()

        when:
        service.createConstraints(TopicName.fromQualifiedName('group.topic'), new Constraints(1, null), USER)

        then:
        def e = thrown(TopicConstraintsAlreadyExistException)
        e.message == 'Constraints for topic group.topic already exist.'

        and:
        assertNodesDoNotExist('group.topic')
    }

    def "nodes should remain unchanged in case of failure while creating subscription constraints"() {
        given:
        setupNode(DC_2_NAME, 'group.topic$sub', new Constraints(2, null))
        ensureCacheWasUpdated()

        when:
        service.createConstraints(SubscriptionName.fromString('group.topic$sub'), new Constraints(1, null), USER)

        then:
        def e = thrown(SubscriptionConstraintsAlreadyExistException)
        e.message == 'Constraints for subscription group.topic$sub already exist.'

        and:
        assertNodesDoNotExist('group.topic$sub')
    }

    def "nodes should remain unchanged in case of unavailability while creating topic constraints"() {
        given:
        zookeeper2.stop()

        when:
        service.createConstraints(TopicName.fromQualifiedName('group.topic'), new Constraints(1, null), USER)

        then:
        def e = thrown(InternalProcessingException)
        e.message == "Execution of command 'CreateTopicConstraints(group.topic)' failed on DC 'dc2'."

        and:
        assertNodeDoesNotExist(DC_1_NAME, 'group.topic')
    }

    def "nodes should remain unchanged in case of unavailability while creating subscription constraints"() {
        given:
        zookeeper2.stop()

        when:
        service.createConstraints(SubscriptionName.fromString('group.topic$sub'), new Constraints(1, null), USER)

        then:
        def e = thrown(InternalProcessingException)
        e.message == "Execution of command 'CreateSubscriptionConstraints(group.topic\$sub)' failed on DC 'dc2'."

        and:
        assertNodeDoesNotExist(DC_1_NAME, 'group.topic$sub')
    }

    def "should update topic constraints in all zk clusters"() {
        given:
        setupNodes('group.topic', new Constraints(1, null))

        when:
        service.updateConstraints(TopicName.fromQualifiedName('group.topic'), new Constraints(2, null), USER)

        then:
        assertNodesContains('group.topic', new Constraints(2, null))
    }

    def "should update subscription constraints in all zk clusters"() {
        given:
        setupNodes('group.topic$sub', new Constraints(1, null))

        when:
        service.updateConstraints(SubscriptionName.fromString('group.topic$sub'), new Constraints(2, null), USER)

        then:
        assertNodesContains('group.topic$sub', new Constraints(2, null))
    }

    def "nodes should remain unchanged in case of failure while updating topic constraints"() {
        given:
        setupNode(DC_1_NAME, 'group.topic', new Constraints(2, null))
        ensureCacheWasUpdated()

        when:
        service.updateConstraints(TopicName.fromQualifiedName('group.topic'), new Constraints(1, null), USER)

        then:
        def e = thrown(TopicConstraintsDoNotExistException)
        e.message == 'Constraints for topic group.topic do not exist.'

        and:
        assertNodeContains(DC_1_NAME, 'group.topic', new Constraints(2, null))
        assertNodeDoesNotExist(DC_2_NAME, 'group.topic')
    }

    def "nodes should remain unchanged in case of failure while updating subscription constraints"() {
        given:
        setupNode(DC_1_NAME, 'group.topic$sub', new Constraints(2, null))
        ensureCacheWasUpdated()

        when:
        service.updateConstraints(SubscriptionName.fromString('group.topic$sub'), new Constraints(1, null), USER)

        then:
        def e = thrown(SubscriptionConstraintsDoNotExistException)
        e.message == 'Constraints for subscription group.topic$sub do not exist.'

        and:
        assertNodeContains(DC_1_NAME, 'group.topic$sub', new Constraints(2, null))
        assertNodeDoesNotExist(DC_2_NAME, 'group.topic$sub')
    }

    def "nodes should remain unchanged in case of unavailability while updating topic constraints"() {
        given:
        setupNode(DC_1_NAME, 'group.topic', new Constraints(2, null))
        ensureCacheWasUpdated()
        zookeeper2.stop()

        when:
        service.updateConstraints(TopicName.fromQualifiedName('group.topic'), new Constraints(1, null), USER)

        then:
        def e = thrown(InternalProcessingException)
        e.message == "Execution of command 'UpdateTopicConstraints(group.topic)' failed on DC 'dc2'."

        and:
        assertNodeContains(DC_1_NAME, 'group.topic', new Constraints(2, null))
    }

    def "nodes should remain unchanged in case of unavailability while updating subscription constraints"() {
        given:
        setupNode(DC_1_NAME, 'group.topic$sub', new Constraints(2, null))
        ensureCacheWasUpdated()
        zookeeper2.stop()

        when:
        service.updateConstraints(SubscriptionName.fromString('group.topic$sub'), new Constraints(1, null), USER)

        then:
        def e = thrown(InternalProcessingException)
        e.message == "Execution of command 'UpdateSubscriptionConstraints(group.topic\$sub)' failed on DC 'dc2'."

        and:
        assertNodeContains(DC_1_NAME, 'group.topic$sub', new Constraints(2, null))
    }

    def "should remove topic constraints in all zk clusters"() {
        given:
        setupNodes('group.topic', new Constraints(1, null))

        when:
        service.deleteConstraints(TopicName.fromQualifiedName('group.topic'), USER)

        then:
        assertNodesDoNotExist('group.topic')
    }

    def "should remove subscription constraints in all zk clusters"() {
        given:
        setupNodes('group.topic$sub', new Constraints(1, null))

        when:
        service.deleteConstraints(SubscriptionName.fromString('group.topic$sub'), USER)

        then:
        assertNodesDoNotExist('group.topic$sub')
    }

    def "should perform remove operation successfully even if some nodes do not have given topic constraints"() {
        given:
        setupNode(DC_1_NAME, 'group.topic', new Constraints(2, null))
        ensureCacheWasUpdated()

        assert assertNodeDoesNotExist(DC_2_NAME, 'group.topic')

        when:
        service.deleteConstraints(TopicName.fromQualifiedName('group.topic'), USER)

        then:
        noExceptionThrown()

        and:
        assertNodesDoNotExist('group.topic')
    }

    def "should perform remove operation successfully even if some nodes do not have given subscription constraints"() {
        given:
        setupNode(DC_1_NAME, 'group.topic$sub', new Constraints(2, null))
        ensureCacheWasUpdated()

        assert assertNodeDoesNotExist(DC_2_NAME, 'group.topic$sub')

        when:
        service.deleteConstraints(SubscriptionName.fromString('group.topic$sub'), USER)

        then:
        noExceptionThrown()

        and:
        assertNodesDoNotExist('group.topic$sub')
    }

    def "nodes should remain unchanged in case of failure while deleting topic constraints"() {
        given:
        setupNode(DC_1_NAME, 'group.topic', new Constraints(2, null))
        ensureCacheWasUpdated()
        zookeeper2.stop()

        when:
        service.deleteConstraints(TopicName.fromQualifiedName('group.topic'), USER)

        then:
        def e = thrown(InternalProcessingException)
        e.message == "Execution of command 'DeleteTopicConstraints(group.topic)' failed on DC 'dc2'."

        and:
        assertNodeContains(DC_1_NAME, 'group.topic', new Constraints(2, null))
    }

    def "nodes should remain unchanged in case of failure while deleting subscription constraints"() {
        given:
        setupNode(DC_1_NAME, 'group.topic$sub', new Constraints(2, null))
        ensureCacheWasUpdated()
        zookeeper2.stop()

        when:
        service.deleteConstraints(SubscriptionName.fromString('group.topic$sub'), USER)

        then:
        def e = thrown(InternalProcessingException)
        e.message == "Execution of command 'DeleteSubscriptionConstraints(group.topic\$sub)' failed on DC 'dc2'."

        and:
        assertNodeContains(DC_1_NAME, 'group.topic$sub', new Constraints(2, null))
    }

    def "should return true if topic constraints exist in local zk, otherwise false"() {
        given:
        setupNodes('group.topic', new Constraints(1, null))
        ensureCacheWasUpdated()

        expect:
        service.constraintsExist(TopicName.fromQualifiedName('group.topic'))
        !service.constraintsExist(TopicName.fromQualifiedName('group.non-existent-topic'))
    }

    def "should return true if subscription constraints exist in local zk, otherwise false"() {
        given:
        setupNodes('group.topic$sub', new Constraints(1, null))
        ensureCacheWasUpdated()

        expect:
        service.constraintsExist(SubscriptionName.fromString('group.topic$sub'))
        !service.constraintsExist(SubscriptionName.fromString('group.topic$non-existent-subscription'))
    }

    private def assertNodeContains(String dc, String path, Constraints expectedConstraints) {
        def zkClient = findClientByDc(manager.clients, dc)
        def data = zkClient.curatorFramework.getData().forPath("$WORKLOAD_CONSTRAINTS_PATH/$path")
        def constraints = objectMapper.readValue(data, Constraints)
        constraints == expectedConstraints
    }

    private def assertNodesContains(String path, Constraints expectedConstraints) {
        manager.clients.each {
            def data = it.curatorFramework.getData().forPath("$WORKLOAD_CONSTRAINTS_PATH/$path")
            def constraints = objectMapper.readValue(data, Constraints)
            assert constraints == expectedConstraints
        }
    }

    private def assertNodeDoesNotExist(String dc, String path) {
        def zkClient = findClientByDc(manager.clients, dc)
        zkClient.curatorFramework.checkExists().forPath("$WORKLOAD_CONSTRAINTS_PATH/$path") == null
    }

    private def assertNodesDoNotExist(String path) {
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

    private def setupNode(String dc, String path, Constraints constraints) {
        def zkClient = findClientByDc(manager.clients, dc)
        zkClient.curatorFramework.create()
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
