//package pl.allegro.tech.hermes.infrastructure.zookeeper
//
//import ch.qos.logback.classic.Level
//import ch.qos.logback.classic.Logger
//import ch.qos.logback.classic.spi.ILoggingEvent
//import ch.qos.logback.core.read.ListAppender
//import com.fasterxml.jackson.databind.ObjectMapper
//import org.slf4j.LoggerFactory
//import pl.allegro.tech.hermes.api.SubscriptionName
//import pl.allegro.tech.hermes.api.TopicName
//import pl.allegro.tech.hermes.api.Constraints
//import pl.allegro.tech.hermes.test.IntegrationTest
//
//import java.util.concurrent.TimeUnit
//
//import static com.jayway.awaitility.Awaitility.await
//
//class ZookeeperWorkloadConstraintsRepositoryTest extends IntegrationTest {
//
//    ZookeeperWorkloadConstraintsRepository repository
//    ZookeeperWorkloadConstraintsCache pathChildrenCache
//    def paths = new ZookeeperPaths("/hermes")
//    def curator = zookeeper()
//
//    Logger logger
//    ListAppender<ILoggingEvent> listAppender
//
//    def setup() {
//        logger = (Logger) LoggerFactory.getLogger(ZookeeperWorkloadConstraintsRepository.class)
//        listAppender = new ListAppender<>()
//        listAppender.start()
//        logger.addAppender(listAppender)
//
//        try {
//            deleteAllNodes("/hermes/consumers-workload-constraints")
//        } catch (Exception e) {
//            e.printStackTrace()
//        }
//
//        pathChildrenCache = new ZookeeperWorkloadConstraintsCache(curator, paths.consumersWorkloadConstraintsPath())
//        repository = new ZookeeperWorkloadConstraintsRepository(curator, new ObjectMapper(), paths, pathChildrenCache)
//    }
//
//    def cleanup() {
//        pathChildrenCache.close()
//    }
//
//    def "should return empty constraints if base path does not exist"() {
//        when:
//        def workloadConstraints = repository.getConsumersWorkloadConstraints()
//
//        then:
//        workloadConstraints.getTopicConstraints().isEmpty()
//        workloadConstraints.getSubscriptionConstraints().isEmpty()
//
//        and:
//        listAppender.list.isEmpty()
//    }
//
//    def "should return constraints for given topic and subscription"() {
//        given:
//        def topic = TopicName.fromQualifiedName('group.topic')
//        def subscription = SubscriptionName.fromString('group.topic$sub');
//
//        setupNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(1))
//        setupNode('/hermes/consumers-workload-constraints/group.topic$sub', new Constraints(3))
//        ensureCacheWasUpdated(2)
//
//        when:
//        def constraints = repository.getConsumersWorkloadConstraints()
//
//        then:
//        def topicConstraints = constraints.getTopicConstraints()
//        topicConstraints.get(topic).getConsumersNumber() == 1
//
//        def subscriptionConstraints = constraints.getSubscriptionConstraints()
//        subscriptionConstraints.get(subscription).getConsumersNumber() == 3
//
//        and:
//        listAppender.list.isEmpty()
//    }
//
//    def "should log warn message if data from ZNode cannot be read"() {
//        given:
//        setupNode('/hermes/consumers-workload-constraints/group.topic', 'random data')
//        ensureCacheWasUpdated(1)
//
//        when:
//        def constraints = repository.getConsumersWorkloadConstraints()
//
//        then:
//        constraints.getTopicConstraints().isEmpty()
//
//        and:
//        listAppender.list.get(0).formattedMessage == "Error while reading data from node /hermes/consumers-workload-constraints/group.topic"
//        listAppender.list.get(0).throwableProxy.className == "com.fasterxml.jackson.databind.exc.MismatchedInputException"
//        listAppender.list.get(0).level == Level.WARN
//    }
//
//    private def ensureCacheWasUpdated(int expectedSize) {
//        await()
//                .atMost(200, TimeUnit.MILLISECONDS)
//                .until { pathChildrenCache.getCurrentData().size() == expectedSize }
//    }
//}
