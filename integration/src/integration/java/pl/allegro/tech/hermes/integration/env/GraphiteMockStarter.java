package pl.allegro.tech.hermes.integration.env;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.integration.helper.graphite.GraphiteMockServer;
import pl.allegro.tech.hermes.test.helper.environment.Starter;

public class GraphiteMockStarter implements Starter<GraphiteMockServer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphiteMockStarter.class);

    private final GraphiteMockServer graphiteMockServer;

    public GraphiteMockStarter(int port) {
        graphiteMockServer = new GraphiteMockServer(port);
    }

    @Override
    public GraphiteMockServer instance() {
        return graphiteMockServer;
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("Starting graphite server");
        graphiteMockServer.start();
    }

    @Override
    public void stop() throws Exception {
        LOGGER.info("Stopping graphite server");
        graphiteMockServer.stop();
    }
}
