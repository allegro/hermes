package pl.allegro.tech.hermes.benchmark.environment;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.shaded.okhttp3.OkHttpClient;
import org.testcontainers.shaded.okhttp3.Request;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.ConfigFactoryConfiguration;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.test.helper.environment.Starter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.TEN_SECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

@State(Scope.Benchmark)
public class FrontendStarter implements Starter<ConfigurableApplicationContext> { //TODO: Duplicate from integration

    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendStarter.class);

    private final int port;
    private final List<String> args = new ArrayList<>();
    private final List<String> profiles = new ArrayList<>();
    private final SpringApplication application = new SpringApplication(HermesFrontend.class);
    private ConfigurableApplicationContext applicationContext;
    private OkHttpClient client;

    public FrontendStarter(int port) {
        application.setWebApplicationType(WebApplicationType.NONE);
        this.port = port;
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting Hermes Frontend");
        client = new OkHttpClient();
        ConfigFactoryConfiguration configFactoryConfiguration = new ConfigFactoryConfiguration();
        setSpringProfilesArg();
        ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        beanFactory.registerSingleton(configFactoryConfiguration.getClass().getCanonicalName(), configFactoryConfiguration);
        applicationContext = application.run(args.toArray(new String[0]));
        waitForStartup();
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping Hermes Frontend");
        instance().close();
    }

    @Override
    public ConfigurableApplicationContext instance() {
        return applicationContext;
    }

    public void overrideProperty(Configs config, Object value) {
        args.add(getArgument(config, value));
    }

    public void addSpringProfiles(String... profiles) {
        String profilesString = Arrays.stream(profiles).collect(Collectors.joining(",", "", ""));
        this.profiles.add(profilesString);
    }

    private void setSpringProfilesArg() {
        String profilesString = profiles.stream().collect(Collectors.joining(",", "", ""));
        args.add("--spring.profiles.active=" + profilesString);
    }

    private void waitForStartup() {
        await().atMost(adjust(TEN_SECONDS)).until(() -> {
            Request request = new Request.Builder()
                    .url("http://localhost:" + port + "/status/ping")
                    .build();

            return client.newCall(request).execute().code() == OK.getStatusCode();
        });
    }

    private static String getArgument(Configs config, Object value) {
        return "--" + config.getName() + "=" + value;
    }
}
