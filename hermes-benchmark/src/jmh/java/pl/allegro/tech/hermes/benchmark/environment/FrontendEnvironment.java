package pl.allegro.tech.hermes.benchmark.environment;

import com.codahale.metrics.MetricRegistry;
import org.apache.commons.io.IOUtils;
import org.apache.curator.framework.CuratorFramework;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.test.helper.environment.KafkaStarter;
import pl.allegro.tech.hermes.test.helper.environment.ZookeeperStarter;

import java.io.IOException;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;
import static pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperDataSaver.save;

@State(Scope.Benchmark)
public class FrontendEnvironment {

    private static final Logger logger = LoggerFactory.getLogger(FrontendEnvironment.class);
    private static final int MAX_CONNECTIONS_PER_ROUTE = 200;
    private static final int ZOOKEEPER_PORT = 2181;

    private HermesFrontend hermesFrontend;
    private ZookeeperStarter zookeeperStarter;
    private KafkaStarter kafkaStarter;
    private HermesPublisher publisher;
    private MetricRegistry metricRegistry;

    public static void main(String[] args) throws Exception {
        new FrontendEnvironment().setupEnvironment();
    }

    @Setup(Level.Trial)
    public void setupEnvironment() throws Exception {
        zookeeperStarter = new ZookeeperStarter(ZOOKEEPER_PORT, "localhost");
        zookeeperStarter.start();

        kafkaStarter = new KafkaStarter("/kafka.properties");
        kafkaStarter.start();

        hermesFrontend = HermesFrontend.frontend().withDisabledGlobalShutdownHook().build();
        hermesFrontend.start();

        GroupRepository groupRepository = hermesFrontend.getService(GroupRepository.class);
        TopicRepository topicRepository = hermesFrontend.getService(TopicRepository.class);
        groupRepository.createGroup(Group.from("bench"));
        Topic topic = topic("bench.topic").withContentType(AVRO).build();
        topicRepository.createTopic(topic);
        saveSchema(
                hermesFrontend.getService(CuratorFramework.class, CuratorType.HERMES),
                hermesFrontend.getService(ZookeeperPaths.class),
                topic,
                loadMessageResource("schema"));
    }

    @Setup(Level.Trial)
    public void setupPublisher() throws Exception {
        metricRegistry = new MetricRegistry();

        String messageBody = loadMessageResource("completeMessage");
        publisher = new HermesPublisher(MAX_CONNECTIONS_PER_ROUTE, "http://localhost:8080/topics/bench.topic", messageBody, metricRegistry);
    }

    @TearDown(Level.Trial)
    public void shutdownServers() throws Exception {
        hermesFrontend.stop();
        kafkaStarter.stop();
        zookeeperStarter.stop();
    }

    @TearDown(Level.Trial)
    public void shutdownPublisherAndReportMetrics() throws Exception {
        reportMetrics();
        publisher.stop();
    }

    public HermesPublisher publisher() {
        return publisher;
    }

    public static String loadMessageResource(String name) throws IOException {
        return IOUtils.toString(FrontendEnvironment.class.getResourceAsStream(String.format("/message/%s.json", name)));
    }

    private void reportMetrics() {
        metricRegistry.getCounters().entrySet().forEach(e -> logger.info(e.getKey() + ": " + e.getValue().getCount()));
    }

    private void saveSchema(CuratorFramework curatorFramework, ZookeeperPaths zkPaths, Topic topic, String schemaSource) {
        save(curatorFramework, zkPaths.topicPath(topic.getName(), "schema"), RawSchema.valueOf(schemaSource).value().getBytes());
    }
}
