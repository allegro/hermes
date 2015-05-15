package pl.allegro.tech.hermes.test.helper.environment;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WireMockStarter implements Starter<WireMockServer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockStarter.class);

    private final int port;

    private WireMockServer wireMockServer;

    public WireMockStarter(int port) {
        this.port = port;
    }

    @Override
    public WireMockServer instance() {
        return wireMockServer;
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting wiremock");
        wireMockServer = new WireMockServer(port);
        wireMockServer.start();
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping wiremock");
        wireMockServer.stop();
    }

}
