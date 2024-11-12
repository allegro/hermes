package pl.allegro.tech.hermes.management.domain.detection

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import pl.allegro.tech.hermes.common.exception.InternalProcessingException
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.management.config.storage.DefaultZookeeperGroupRepositoryFactory
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor
import pl.allegro.tech.hermes.management.domain.mode.ModeService
import pl.allegro.tech.hermes.management.infrastructure.detection.ZookeeperInactiveTopicsRepository
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperRepositoryManager
import pl.allegro.tech.hermes.management.utils.MultiZookeeperIntegrationTest

class InactiveTopicsStorageServiceTest extends MultiZookeeperIntegrationTest {

    static INACTIVE_TOPICS_PATH = '/hermes/inactive-topics'

    ZookeeperClientManager manager
    ZookeeperRepositoryManager repositoryManager
    ModeService modeService
    MultiDatacenterRepositoryCommandExecutor commandExecutor
    InactiveTopicsStorageService inactiveTopicsStorageService
    InactiveTopicsRepository inactiveTopicsRepository

    def objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
    def paths = new ZookeeperPaths('/hermes')

    def setup() {
        manager = buildZookeeperClientManager()
        manager.start()
        assertZookeeperClientsConnected(manager.clients)
        inactiveTopicsRepository = new ZookeeperInactiveTopicsRepository(manager.localClient.curatorFramework, objectMapper, paths)
        repositoryManager = new ZookeeperRepositoryManager(
                manager, new TestDatacenterNameProvider(DC_1_NAME), objectMapper,
                paths, new DefaultZookeeperGroupRepositoryFactory())
        repositoryManager.start()
        modeService = new ModeService()
        commandExecutor = new MultiDatacenterRepositoryCommandExecutor(repositoryManager, true, modeService)
        inactiveTopicsStorageService = new InactiveTopicsStorageService(inactiveTopicsRepository, commandExecutor)
    }

    def cleanup() {
        manager.stop()
    }

    def TEST_INACTIVE_TOPICS = [
            new InactiveTopic("group.topic1", 1730641716154L, [1730641716250L, 1730641716321], false),
            new InactiveTopic("group.topic2", 1730641712371L, [1730641716250L], false),
            new InactiveTopic("group.topic3", 1730641712371L, [], true),
    ]

    def "should create node in all zk clusters if it doesn't exist when upserting"() {
        when:
        inactiveTopicsStorageService.markAsInactive(TEST_INACTIVE_TOPICS)

        then:
        assertNodesContain(TEST_INACTIVE_TOPICS)
    }

    def "should update existing node data in all zk clusters when upserting"() {
        given:
        setupNodes(TEST_INACTIVE_TOPICS)

        and:
        def newInactiveTopics = [
                TEST_INACTIVE_TOPICS[0],
                new InactiveTopic("group.topic3", 1730641712371L, [1730641712678L], false),
                new InactiveTopic("group.topic4", 1730641712706L, [1730641712999L], false),
        ]

        when:
        inactiveTopicsStorageService.markAsInactive(newInactiveTopics)

        then:
        assertNodesContain(newInactiveTopics)
    }

    def "nodes should remain unchanged in case of unavailability when upserting"() {
        given:
        setupNodes(TEST_INACTIVE_TOPICS)

        and:
        zookeeper2.stop()

        when:
        inactiveTopicsStorageService.markAsInactive([
                new InactiveTopic("group.topic3", 1730641656154L, [], false)
        ])

        then:
        def e = thrown(InternalProcessingException)
        e.message == "Execution of command 'MarkTopicsAsInactive(number of topics=1)' failed on DC 'dc2'."

        and:
        nodeData(DC_1_NAME) == TEST_INACTIVE_TOPICS
    }

    def "should return empty list when node doesn't exist"() {
        when:
        def result = inactiveTopicsStorageService.getInactiveTopics()

        then:
        result == []
    }

    def "should return empty list when node is empty"() {
        given:
        setupNodes([])

        when:
        def result = inactiveTopicsStorageService.getInactiveTopics()

        then:
        result == []
    }

    def "should return list of inactive topics"() {
        given:
        setupNodes(TEST_INACTIVE_TOPICS)

        when:
        def result = inactiveTopicsStorageService.getInactiveTopics()

        then:
        result.sort() == TEST_INACTIVE_TOPICS.sort()
    }

    private def setupNodes(List<InactiveTopic> inactiveTopics) {
        manager.clients.each {
            it.curatorFramework.create()
                    .creatingParentsIfNeeded()
                    .forPath(INACTIVE_TOPICS_PATH, objectMapper.writeValueAsBytes(inactiveTopics))
        }
    }

    private def assertNodesContain(List<InactiveTopic> inactiveTopics) {
        manager.clients.each {
            def data = it.curatorFramework.getData().forPath(INACTIVE_TOPICS_PATH)
            def foundInactiveTopics = objectMapper.readValue(data, new TypeReference<List<InactiveTopic>>() {})
            assert inactiveTopics.sort() == foundInactiveTopics.sort()
        }
    }

    private List<InactiveTopic> nodeData(String dc) {
        def zkClient = findClientByDc(manager.clients, dc)
        def data = zkClient.curatorFramework.getData().forPath(INACTIVE_TOPICS_PATH)
        return objectMapper.readValue(data, new TypeReference<List<InactiveTopic>>() {})
    }
}
