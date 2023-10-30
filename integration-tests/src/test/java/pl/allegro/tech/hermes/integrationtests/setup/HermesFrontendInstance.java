package pl.allegro.tech.hermes.integrationtests.setup;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.test.helper.environment.Starter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Duration.TEN_SECONDS;
import static jakarta.ws.rs.core.Response.Status.OK;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class HermesFrontendInstance implements Starter<ConfigurableApplicationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HermesFrontendInstance.class);

    private final int port;
    private final List<String> args = new ArrayList<>();
    private final List<String> profiles = new ArrayList<>();
    private final SpringApplication application = new SpringApplication(HermesFrontend.class);
    private ConfigurableApplicationContext applicationContext;
    private OkHttpClient client;

    public HermesFrontendInstance(int port) {
        application.setWebApplicationType(WebApplicationType.NONE);
        this.port = port;
        addSpringProfiles("integration");
    }

    private HermesFrontendInstance(int port, List<String> args) {
        this(port);
        this.args.addAll(args);
    }

    public static HermesFrontendInstance withCommonIntegrationTestConfig(int port) {
        return new HermesFrontendInstance(port, commonIntegrationTestConfig(port));
    }

    public static HermesFrontendInstance withCommonIntegrationTestConfig(int port, boolean sslEnabled) {
        List<String> args = commonIntegrationTestConfig(port);
        return new HermesFrontendInstance(port, args);
    }

    private static List<String> commonIntegrationTestConfig(int port) {
        List<String> args = new ArrayList<>();
        args.add(getArgument("frontend.server.port", port));
        return args;
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting Hermes Frontend");
        client = new OkHttpClient();
        setSpringProfilesArg();
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

    public void overrideProperty(String config, Object value) {
        args.add("--" + config + "=" + value);
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
        Request request = new Request.Builder()
                .url("http://localhost:" + port + "/status/ping")
                .build();

        await().atMost(adjust(TEN_SECONDS)).until(() -> client.newCall(request).execute().code() == OK.getStatusCode());
    }

    private static String getArgument(String config, Object value) {
        return "--" + config + "=" + value;
    }
}
