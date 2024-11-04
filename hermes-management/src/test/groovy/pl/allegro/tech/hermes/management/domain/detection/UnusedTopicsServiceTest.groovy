package pl.allegro.tech.hermes.management.domain.detection

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import pl.allegro.tech.hermes.common.exception.InternalProcessingException
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.management.config.storage.DefaultZookeeperGroupRepositoryFactory
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor
import pl.allegro.tech.hermes.management.domain.mode.ModeService
import pl.allegro.tech.hermes.management.infrastructure.detection.ZookeeperUnusedTopicsRepository
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperRepositoryManager
import pl.allegro.tech.hermes.management.utils.MultiZookeeperIntegrationTest

class UnusedTopicsServiceTest extends MultiZookeeperIntegrationTest {

    static UNUSED_TOPICS_PATH = '/hermes/unused-topics'

    ZookeeperClientManager manager
    ZookeeperRepositoryManager repositoryManager
    ModeService modeService
    MultiDatacenterRepositoryCommandExecutor commandExecutor
    UnusedTopicsService unusedTopicsService
    UnusedTopicsRepository unusedTopicsRepository

    def objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).registerModule(new Jdk8Module())
    def paths = new ZookeeperPaths('/hermes')

    def setup() {
        manager = buildZookeeperClientManager()
        manager.start()
        assertZookeeperClientsConnected(manager.clients)
        unusedTopicsRepository = new ZookeeperUnusedTopicsRepository(manager.localClient.curatorFramework, objectMapper, paths)
        repositoryManager = new ZookeeperRepositoryManager(
                manager, new TestDatacenterNameProvider(DC_1_NAME), objectMapper,
                paths, new DefaultZookeeperGroupRepositoryFactory())
        repositoryManager.start()
        modeService = new ModeService()
        commandExecutor = new MultiDatacenterRepositoryCommandExecutor(repositoryManager, true, modeService)
        unusedTopicsService = new UnusedTopicsService(unusedTopicsRepository, commandExecutor)
    }

    def cleanup() {
        manager.stop()
    }

    def TEST_UNUSED_TOPICS = [
            new UnusedTopic("group.topic1", 1730641716154L, Optional.of(1730641716250L), false),
            new UnusedTopic("group.topic2", 1730641712371L, Optional.empty(), true),
    ]

    def "should create node in all zk clusters if it doesn't exist when upserting"() {
        when:
        unusedTopicsService.markAsUnused(TEST_UNUSED_TOPICS)

        then:
        assertNodesContain(TEST_UNUSED_TOPICS)
    }

    def "should update existing node data in all zk clusters when upserting"() {
        given:
        setupNodes(TEST_UNUSED_TOPICS)

        and:
        def newUnusedTopics = [
                TEST_UNUSED_TOPICS[0],
                new UnusedTopic("group.topic2", 1730641712371L, Optional.of(1730641712678L), false),
                new UnusedTopic("group.topic3", 1730641712706L, Optional.of(1730641712999L), false),
        ]

        when:
        unusedTopicsService.markAsUnused(newUnusedTopics)

        then:
        assertNodesContain(newUnusedTopics)
    }

    def "nodes should remain unchanged in case of unavailability when upserting"() {
        given:
        setupNodes(TEST_UNUSED_TOPICS)

        and:
        zookeeper2.stop()

        when:
        unusedTopicsService.markAsUnused([
                new UnusedTopic("group.topic3", 1730641656154L, Optional.empty(), false)
        ])

        then:
        def e = thrown(InternalProcessingException)
        e.message == "Execution of command 'MarkTopicsAsUnused(number of topics=1)' failed on DC 'dc2'."

        and:
        nodeData(DC_1_NAME) == TEST_UNUSED_TOPICS
    }

    def "should return empty list when node doesn't exist"() {
        when:
        def result = unusedTopicsService.getUnusedTopics()

        then:
        result == []
    }

    def "should return empty list when node is empty"() {
        given:
        setupNodes([])

        when:
        def result = unusedTopicsService.getUnusedTopics()

        then:
        result == []
    }

    def "should return list of unused topics"() {
        given:
        setupNodes(TEST_UNUSED_TOPICS)

        when:
        def result = unusedTopicsService.getUnusedTopics()

        then:
        result.sort() == TEST_UNUSED_TOPICS.sort()
    }

    private def setupNodes(List<UnusedTopic> unusedTopics) {
        manager.clients.each {
            it.curatorFramework.create()
                    .creatingParentsIfNeeded()
                    .forPath(UNUSED_TOPICS_PATH, objectMapper.writeValueAsBytes(unusedTopics))
        }
    }

    private def assertNodesContain(List<UnusedTopic> unusedTopics) {
        manager.clients.each {
            def data = it.curatorFramework.getData().forPath(UNUSED_TOPICS_PATH)
            def foundUnusedTopics = objectMapper.readValue(data, new TypeReference<List<UnusedTopic>>() {})
            assert unusedTopics.sort() == foundUnusedTopics.sort()
        }
    }

    private List<UnusedTopic> nodeData(String dc) {
        def zkClient = findClientByDc(manager.clients, dc)
        def data = zkClient.curatorFramework.getData().forPath(UNUSED_TOPICS_PATH)
        return objectMapper.readValue(data, new TypeReference<List<UnusedTopic>>() {})
    }
}
