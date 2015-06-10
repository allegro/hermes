package pl.allegro.tech.hermes.integration.env;

import com.jayway.awaitility.Duration;
import org.eclipse.jetty.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.message.tracker.mongo.frontend.MongoLogRepository;
import pl.allegro.tech.hermes.test.helper.environment.Starter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.jayway.awaitility.Awaitility.await;

public class FrontendStarter implements Starter<HermesFrontend> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendStarter.class);

    private final MutableConfigFactory configFactory;
    private final String frontendUrl;
    private HermesFrontend hermesFrontend;


    public FrontendStarter(String frontendUrl) {
        this.frontendUrl = frontendUrl;
        configFactory = new MutableConfigFactory();
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting Hermes Frontend");
        hermesFrontend = HermesFrontend.frontend()
            .withBinding(configFactory, ConfigFactory.class)
            .withLogRepository(
                    new MongoLogRepository(FongoFactory.hermesDB(), 10, 1000, configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME)))
            .build();

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

    public void overrideProperty(Configs config, Object value) {
        configFactory.overrideProperty(config, value);
    }

    private void waitForStartup() throws Exception {
        final HttpClient httpClient = HttpClientFactory.create();

        await().atMost(Duration.TEN_SECONDS).until(() -> {
            try {
                return httpClient.GET(frontendUrl).getStatus() == 200;
            } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                return false;
            }
        });
    }
}
