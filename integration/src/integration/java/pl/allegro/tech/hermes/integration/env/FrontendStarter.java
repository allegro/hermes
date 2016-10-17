package pl.allegro.tech.hermes.integration.env;

import com.codahale.metrics.MetricRegistry;
import com.jayway.awaitility.Duration;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.integration.metadata.TraceHeadersPropagator;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.environment.Starter;
import pl.allegro.tech.hermes.tracker.mongo.frontend.MongoLogRepository;

import static com.jayway.awaitility.Awaitility.await;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_PORT;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_SSL_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_REPOSITORY_TYPE;
import static pl.allegro.tech.hermes.common.schema.SchemaRepositoryType.SCHEMA_REGISTRY;

public class FrontendStarter implements Starter<HermesFrontend> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendStarter.class);

    private final MutableConfigFactory configFactory;
    private final int port;
    private HermesFrontend hermesFrontend;
    private OkHttpClient client;


    public FrontendStarter(int port) {
        this.port = port;
        configFactory = new MutableConfigFactory();
        configFactory.overrideProperty(FRONTEND_PORT, port);
        configFactory.overrideProperty(SCHEMA_REPOSITORY_TYPE, SCHEMA_REGISTRY.name());
        configFactory.overrideProperty(SCHEMA_CACHE_ENABLED, false);
    }

    public FrontendStarter(int port, boolean sslEnabled) {
        this(port);
        configFactory.overrideProperty(FRONTEND_SSL_ENABLED, sslEnabled);
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting Hermes Frontend");
        hermesFrontend = HermesFrontend.frontend()
            .withBinding(configFactory, ConfigFactory.class)
            .withHeadersPropagator(new TraceHeadersPropagator())
            .withLogRepository(serviceLocator -> new MongoLogRepository(FongoFactory.hermesDB(),
                    10,
                    1000,
                    configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME),
                    configFactory.getStringProperty(Configs.HOSTNAME),
                    serviceLocator.getService(MetricRegistry.class),
                    serviceLocator.getService(PathsCompiler.class)))
            .withKafkaTopicsNamesMapper(serviceLocator ->
                    new IntegrationTestKafkaNamesMapperFactory(configFactory.getStringProperty(Configs.KAFKA_NAMESPACE)).create())
            .withDisabledGlobalShutdownHook()
            .build();

        client = new OkHttpClient();
        hermesFrontend.start();
        waitForStartup();
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping Hermes Frontend");
        hermesFrontend.stop();
    }

    @Override
    public HermesFrontend instance() {
        return hermesFrontend;
    }

    public ConfigFactory config() {
        return configFactory;
    }

    public void overrideProperty(Configs config, Object value) {
        configFactory.overrideProperty(config, value);
    }

    private void waitForStartup() throws Exception {

        await().atMost(Duration.TEN_SECONDS).until(() -> {
            Request request = new Request.Builder()
                    .url("http://localhost:" + port)
                    .build();

            return client.newCall(request).execute().code() == 200;
        });
    }
}
