package pl.allegro.tech.hermes.benchmark.environment;

import com.codahale.metrics.MetricRegistry;
import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.environment.KafkaStarter;
import pl.allegro.tech.hermes.test.helper.environment.ZookeeperStarter;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import java.io.IOException;
import java.util.Properties;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

@State(Scope.Benchmark)
public class FrontendEnvironment {

    private static final Logger logger = LoggerFactory.getLogger(FrontendEnvironment.class);
    private static final int MAX_CONNECTIONS_PER_ROUTE = 200;
    private static final int ZOOKEEPER_PORT = Ports.nextAvailable();

    private HermesFrontend hermesFrontend;
    private ZookeeperStarter zookeeperStarter;
    private KafkaStarter kafkaStarter;
    private HermesPublisher publisher;
    private MetricRegistry metricRegistry;

    public static void main(String[] args) throws Exception {
        FrontendEnvironment env =  new FrontendEnvironment();
        env.setupEnvironment();
        env.shutdownServers();
    }

    @Setup(Level.Trial)
    public void setupEnvironment() throws Exception {
        String zookeeperConnectString = "localhost:" + ZOOKEEPER_PORT;
        zookeeperStarter = new ZookeeperStarter(ZOOKEEPER_PORT,zookeeperConnectString );
        zookeeperStarter.start();

        Properties kafkaProperties = new Properties();
        kafkaProperties.load(this.getClass().getResourceAsStream("/kafka.properties"));
        kafkaProperties.setProperty("zookeeper.connect",zookeeperConnectString);

        kafkaStarter = new KafkaStarter(kafkaProperties);
        kafkaStarter.start();

        ConfigFactory configFactory = new MutableConfigFactory()
                .overrideProperty(Configs.ZOOKEEPER_CONNECT_STRING, zookeeperConnectString);

        hermesFrontend = HermesFrontend.frontend()
                .withBinding(configFactory, ConfigFactory.class)
                .withDisabledGlobalShutdownHook()
                .withDisabledFlushLogsShutdownHook()
                .withBinding(
                        new InMemorySchemaClient(fromQualifiedName("bench.topic"), loadMessageResource("schema"), 1, 1),
                        RawSchemaClient.class)
                .build();
        hermesFrontend.start();

        GroupRepository groupRepository = hermesFrontend.getService(GroupRepository.class);
        TopicRepository topicRepository = hermesFrontend.getService(TopicRepository.class);
        groupRepository.createGroup(Group.from("bench"));
        Topic topic = topic("bench.topic").withContentType(AVRO).build();
        topicRepository.createTopic(topic);
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
}
