package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ZookeeperWorkloadConstraintsRepositoryTest extends ZookeeperBaseTest {

    private ZookeeperWorkloadConstraintsRepository repository;
    private ZookeeperWorkloadConstraintsPathChildrenCache pathChildrenCache;
    private ZookeeperPaths paths = new ZookeeperPaths("/hermes");

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @Before
    public void setup() {
        logger = (Logger) LoggerFactory.getLogger(ZookeeperWorkloadConstraintsRepository.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            deleteAllNodes();
        } catch (Exception e) {
            e.printStackTrace();
        }

        pathChildrenCache = new ZookeeperWorkloadConstraintsPathChildrenCache(zookeeperClient, paths.consumersWorkloadConstraintsPath());
        repository = new ZookeeperWorkloadConstraintsRepository(zookeeperClient, new ObjectMapper(), paths, pathChildrenCache);
    }

    @After
    public void cleanup() throws Exception {
        pathChildrenCache.close();
    }

    @Test
    public void shouldReturnEmptyConstraintsIfBasePathDoesNotExistTest() {
        // when
        final ConsumersWorkloadConstraints workloadConstraints = repository.getConsumersWorkloadConstraints();

        // then
        assertThat(workloadConstraints.getTopicConstraints()).isEqualTo(emptyMap());
        assertThat(workloadConstraints.getSubscriptionConstraints()).isEqualTo(emptyMap());

        // and
        assertThat(listAppender.list).isEmpty();
    }

    @Test
    public void shouldReturnConstraintsForGivenTopicAndSubscriptionTest() throws Exception {
        // given
        TopicName topic = TopicName.fromQualifiedName("group.topic");
        SubscriptionName subscription = SubscriptionName.fromString("group.topic$sub");

        setupNode("/hermes/consumers-workload-constraints/group.topic", new Constraints(1));
        setupNode("/hermes/consumers-workload-constraints/group.topic$sub", new Constraints(3));
        ensureCacheWasUpdated(2);

        // when
        ConsumersWorkloadConstraints constraints = repository.getConsumersWorkloadConstraints();

        // then
        Map<TopicName, Constraints> topicConstraints = constraints.getTopicConstraints();
        assertThat(topicConstraints.get(topic).getConsumersNumber()).isEqualTo(1);

        Map<SubscriptionName, Constraints> subscriptionConstraints = constraints.getSubscriptionConstraints();
        assertThat(subscriptionConstraints.get(subscription).getConsumersNumber()).isEqualTo(3);

        // and
        assertThat(listAppender.list).isEmpty();
    }

    @Test
    public void shouldLogWarnMessageIfDataFromZNodeCannotBeReadTest() throws Exception {
        // given
        setupNode("/hermes/consumers-workload-constraints/group.topic", "random data");
        ensureCacheWasUpdated(1);

        // when
        ConsumersWorkloadConstraints constraints = repository.getConsumersWorkloadConstraints();

        // then
        assertThat(constraints.getTopicConstraints()).isEqualTo(emptyMap());

        // and
        assertThat(listAppender.list.get(0).getFormattedMessage())
                .isEqualTo("Error while reading data from node /hermes/consumers-workload-constraints/group.topic");
        assertThat(listAppender.list.get(0).getThrowableProxy().getClassName())
                .isEqualTo("com.fasterxml.jackson.databind.exc.MismatchedInputException");
        assertThat(listAppender.list.get(0).getLevel())
                .isEqualTo(Level.WARN);
    }

    private void ensureCacheWasUpdated(int expectedSize) {
        await()
                .atMost(200, TimeUnit.MILLISECONDS)
                .until(() -> pathChildrenCache.getChildrenData().size() == expectedSize);
    }
}
