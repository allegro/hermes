package pl.allegro.tech.hermes.frontend.di;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

public class LoggerConfiguration {
    public static final String UNDERTOW_ERROR_RESPONSE_LOGGER = "io.undertow.request.error-response";

    public static void disableResponseDebugLogger() {
        ((Logger) LoggerFactory.getLogger(UNDERTOW_ERROR_RESPONSE_LOGGER))
                .setLevel(Level.INFO);
    }
}
