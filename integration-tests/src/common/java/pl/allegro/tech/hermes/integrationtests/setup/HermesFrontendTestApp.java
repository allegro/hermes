package pl.allegro.tech.hermes.integrationtests.setup;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_FORCE_TOPIC_MAX_MESSAGE_SIZE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_HEADER_PROPAGATION_ALLOWED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_HEADER_PROPAGATION_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_IDLE_TIMEOUT;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_PORT;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_READINESS_CHECK_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_READINESS_CHECK_INTERVAL_SECONDS;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_READINESS_CHECK_KAFKA_CHECK_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_THROUGHPUT_FIXED_MAX;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_THROUGHPUT_TYPE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.KAFKA_BROKER_LIST;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.KAFKA_NAMESPACE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.KAFKA_PRODUCER_METADATA_MAX_AGE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.SCHEMA_CACHE_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.SCHEMA_REPOSITORY_SERVER_URL;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.SPRING_PROFILES_ACTIVE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.ZOOKEEPER_CONNECTION_STRING;

public class HermesFrontendTestApp implements HermesTestApp {

    private final ZookeeperContainer hermesZookeeper;
    private final KafkaContainerCluster kafka;
    private final ConfluentSchemaRegistryContainer schemaRegistry;
    private final SpringApplicationBuilder app = new SpringApplicationBuilder(HermesFrontend.class)
            .web(WebApplicationType.NONE);

    private int port = -1;
    private boolean kafkaCheckEnabled = false;
    private Duration metadataMaxAge = Duration.ofMinutes(5);
    private Duration readinessCheckInterval = Duration.ofSeconds(1);
    private final List<String> extraArgs = new ArrayList<>();

    public HermesFrontendTestApp(ZookeeperContainer hermesZookeeper,
                                 KafkaContainerCluster kafka,
                                 ConfluentSchemaRegistryContainer schemaRegistry) {
        this.hermesZookeeper = hermesZookeeper;
        this.kafka = kafka;
        this.schemaRegistry = schemaRegistry;
    }

    public HermesFrontendTestApp withProperty(String name, Object value) {
        this.extraArgs.add(getArgument(name, value));
        return this;
    }


    private  List<String> defaultFrontendArgs() {
        List<String> args = new ArrayList<>();
        args.add(getArgument(SPRING_PROFILES_ACTIVE, "integration"));
        args.add(getArgument(FRONTEND_PORT, 0));

        args.add(getArgument(KAFKA_NAMESPACE, "itTest"));
        args.add(getArgument(KAFKA_BROKER_LIST, kafka.getBootstrapServersForExternalClients()));

        args.add(getArgument(ZOOKEEPER_CONNECTION_STRING, hermesZookeeper.getConnectionString()));

        args.add(getArgument(SCHEMA_CACHE_ENABLED, true));
        args.add(getArgument(SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl()));

        args.add(getArgument(FRONTEND_READINESS_CHECK_KAFKA_CHECK_ENABLED, kafkaCheckEnabled));
        args.add(getArgument(FRONTEND_READINESS_CHECK_ENABLED, true));
        args.add(getArgument(FRONTEND_READINESS_CHECK_INTERVAL_SECONDS, readinessCheckInterval));

        args.add(getArgument(FRONTEND_HEADER_PROPAGATION_ENABLED, true));
        args.add(getArgument(FRONTEND_HEADER_PROPAGATION_ALLOWED, "Trace-Id, Span-Id, Parent-Span-Id, Trace-Sampled, Trace-Reported"));

        args.add(getArgument(KAFKA_PRODUCER_METADATA_MAX_AGE, metadataMaxAge));

        args.add(getArgument(FRONTEND_FORCE_TOPIC_MAX_MESSAGE_SIZE,true));
        args.add(getArgument(FRONTEND_IDLE_TIMEOUT, Duration.ofSeconds(2)));

        args.add(getArgument(FRONTEND_THROUGHPUT_TYPE, "fixed"));
        args.add(getArgument(FRONTEND_THROUGHPUT_FIXED_MAX,  50 * 1024L));

        return args;
    }


    @Override
    public HermesTestApp start() {
        List<String> args = defaultFrontendArgs();
        args.addAll(extraArgs);

        app.run(args.toArray(new String[0]));

        port = app.context().getBean(HermesServer.class).getPort();
        return this;
    }

    @Override
    public void stop() {
        app.context().close();
    }

    @Override
    public int getPort() {
        if (port == -1) {
            throw new IllegalStateException("hermes-frontend port hasn't been initialized");
        }
        return port;
    }

    public <T> T getBean(Class<T> clazz) {
        return app.context().getBean(clazz);
    }

    public HermesFrontendTestApp metadataMaxAgeInSeconds(int value) {
        metadataMaxAge = Duration.ofSeconds(value);
        return this;
    }

    public HermesFrontendTestApp readinessCheckIntervalInSeconds(int value) {
        readinessCheckInterval = Duration.ofSeconds(value);
        return this;
    }

    public HermesFrontendTestApp kafkaCheckEnabled() {
        kafkaCheckEnabled = true;
        return this;
    }

    public HermesFrontendTestApp kafkaCheckDisabled() {
        kafkaCheckEnabled = false;
        return this;
    }

    private static String getArgument(String config, Object value) {
        return "--" + config + "=" + value;
    }
}
