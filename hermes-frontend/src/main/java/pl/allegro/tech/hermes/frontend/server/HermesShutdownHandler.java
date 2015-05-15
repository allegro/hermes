package pl.allegro.tech.hermes.frontend.server;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

public class HermesShutdownHandler extends GracefulShutdownHandler {

    private static final int MILLIS = 1000;
    private static final Logger LOGGER = LoggerFactory.getLogger(HermesShutdownHandler.class);

    private final HermesMetrics metrics;

    public HermesShutdownHandler(HttpHandler next, HermesMetrics metrics) {
        super(next);
        this.metrics = metrics;
    }

    public void handleShutdown() throws InterruptedException {
        shutdown();
        awaitShutdown();
        LOGGER.info("Awaiting buffer flush");
        awaitBufferFlush();
    }

    private void awaitBufferFlush() throws InterruptedException {
        while (!isBufferEmpty()) {
            Thread.sleep(MILLIS);
        }
    }

    private boolean isBufferEmpty() {
        LOGGER.info("Available {}, total {} bytes", metrics.getBufferAvailablesBytes(), metrics.getBufferTotalBytes());
        return metrics.getBufferAvailablesBytes() == metrics.getBufferTotalBytes();
    }
}