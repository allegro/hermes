package pl.allegro.tech.hermes.integration.env;

import com.codahale.metrics.MetricRegistry;
import com.google.common.io.Files;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.HermesConsumers;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.integration.metadata.TraceHeadersPropagator;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.environment.Starter;
import pl.allegro.tech.hermes.tracker.mongo.frontend.MongoLogRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.TEN_SECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_FORCE_TOPIC_MAX_MESSAGE_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_GRACEFUL_SHUTDOWN_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_PORT;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_SSL_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_THROUGHPUT_FIXED_MAX;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_THROUGHPUT_TYPE;
import static pl.allegro.tech.hermes.common.config.Configs.SCHEMA_CACHE_ENABLED;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class FrontendStarter implements Starter<ConfigurableApplicationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendStarter.class);

//    private final MutableConfigFactory configFactory;
    private final int port;
    private final List<String> args;
    private final SpringApplication application = new SpringApplication(HermesConsumers.class);
    private ConfigurableApplicationContext applicationContext;
//    private HermesFrontend hermesFrontend;
    private OkHttpClient client;

    public FrontendStarter(int port) {
        args = new ArrayList<>();
        application.setWebApplicationType(WebApplicationType.NONE);
        this.port = port;
        setSpringProfiles("integration");//TODO
//        overrideProperty(FRONTEND_PORT, port);
//        overrideProperty(SCHEMA_CACHE_ENABLED, true);
//        overrideProperty(FRONTEND_FORCE_TOPIC_MAX_MESSAGE_SIZE, true);
//        overrideProperty(FRONTEND_THROUGHPUT_TYPE, "fixed");
//        overrideProperty(FRONTEND_THROUGHPUT_FIXED_MAX, 50 * 1024L);
//        overrideProperty(FRONTEND_GRACEFUL_SHUTDOWN_ENABLED, false);
    }

//    public FrontendStarter(int port, boolean sslEnabled) {
//        this(port);
//        overrideProperty(FRONTEND_SSL_ENABLED, sslEnabled);
//    }

    public FrontendStarter() {
        this(-1);
    }

    public FrontendStarter(int port, List<String> args) {
        this(port);
        this.args.addAll(args);
    }

    public static FrontendStarter withCommonIntegrationTestConfig(int port) {
        return new FrontendStarter(port, commonIntegrationTestConfig(port));
    }

    public static FrontendStarter withCommonIntegrationTestConfig(int port, boolean sslEnabled) {
        List<String> args = commonIntegrationTestConfig(port);
        args.add(getArgument(FRONTEND_SSL_ENABLED, sslEnabled));
        return new FrontendStarter(port, args);
    }

    private static List<String> commonIntegrationTestConfig(int port) {
        List<String> args = new ArrayList<>();
        args.add(getArgument(FRONTEND_PORT, port));
        args.add(getArgument(SCHEMA_CACHE_ENABLED, true));
        args.add(getArgument(FRONTEND_FORCE_TOPIC_MAX_MESSAGE_SIZE, true));
        args.add(getArgument(FRONTEND_THROUGHPUT_TYPE, "fixed"));
        args.add(getArgument(FRONTEND_THROUGHPUT_FIXED_MAX, 50 * 1024L));
        args.add(getArgument(FRONTEND_GRACEFUL_SHUTDOWN_ENABLED, false));
        return args;
    }


//    public FrontendStarter(int port) {
//        this.port = port;
//        configFactory = new MutableConfigFactory();
//        configFactory.overrideProperty(FRONTEND_PORT, port);
//        configFactory.overrideProperty(SCHEMA_CACHE_ENABLED, true);
//        configFactory.overrideProperty(FRONTEND_FORCE_TOPIC_MAX_MESSAGE_SIZE, true);
//        configFactory.overrideProperty(FRONTEND_THROUGHPUT_TYPE, "fixed");
//        configFactory.overrideProperty(FRONTEND_THROUGHPUT_FIXED_MAX, 50 * 1024L);
//        configFactory.overrideProperty(FRONTEND_GRACEFUL_SHUTDOWN_ENABLED, false);
//    }
//
//    public FrontendStarter(int port, boolean sslEnabled) {
//        this(port);
//        configFactory.overrideProperty(FRONTEND_SSL_ENABLED, sslEnabled);
//    }

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting Hermes Frontend");
//        hermesFrontend = HermesFrontend.frontend()
//            .withBinding(configFactory, ConfigFactory.class)
//            .withHeadersPropagator(new TraceHeadersPropagator())
//            .withLogRepository(serviceLocator -> new MongoLogRepository(FongoFactory.hermesDB(),
//                    10,
//                    1000,
//                    configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME),
//                    configFactory.getStringProperty(Configs.HOSTNAME),
//                    serviceLocator.getService(MetricRegistry.class),
//                    serviceLocator.getService(PathsCompiler.class)))
//            .withKafkaTopicsNamesMapper(
//                    new IntegrationTestKafkaNamesMapperFactory(configFactory.getStringProperty(Configs.KAFKA_NAMESPACE)).create())
//            .withDisabledGlobalShutdownHook()//TODO
//            .withDisabledFlushLogsShutdownHook()//TODO
//            .build();

        client = new OkHttpClient();
//        hermesFrontend.start();
        applicationContext = application.run(args.toArray(new String[0]));
        waitForStartup();//TODO - remove, use another?
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping Hermes Frontend");
        instance().stop();
    }

    @Override
    public ConfigurableApplicationContext instance() {
        return applicationContext;
    }

//    public ConfigFactory config() {
//        return configFactory;
//    }

//    public void overrideProperty(Configs config, Object value) {
//        configFactory.overrideProperty(config, value);
//    }

    public void overrideProperty(Configs config, Object value) {
//        args.add("--" + config.getName() + "=" + value);
        args.add(getArgument(config, value));
    }

    public void setSpringProfiles(String... profiles) {
        String profilesString = Arrays.stream(profiles).collect(Collectors.joining(",", "", ""));
        args.add("--spring.profiles.active=" + profilesString);
    }

    private void waitForStartup() throws Exception {
        if(port != -1) {//TODO - when it should run
            await().atMost(adjust(TEN_SECONDS)).until(() -> {
                Request request = new Request.Builder()
                        .url("http://localhost:" + port + "/status/ping")
                        .build();

                return client.newCall(request).execute().code() == OK.getStatusCode();
            });
        }
    }

    private static String getArgument(Configs config, Object value) {
        return "--" + config.getName() + "=" + value;
    }
}
