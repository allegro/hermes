package pl.allegro.tech.hermes.frontend.config;

import pl.allegro.tech.hermes.frontend.server.ContextFactoryParameters;
import pl.allegro.tech.hermes.frontend.server.HermesServerParameters;

public class ConfigPropertiesFactory {

    static ContextFactoryParameters createContextFactoryParameters(FrontendSslProperties frontendSslProperties) {
        return new ContextFactoryParameters(
                frontendSslProperties.getProtocol(),
                frontendSslProperties.getKeystore().getSource(),
                frontendSslProperties.getKeystore().getLocation(),
                frontendSslProperties.getKeystore().getFormat(),
                frontendSslProperties.getKeystore().getPassword(),
                frontendSslProperties.getTruststore().getSource(),
                frontendSslProperties.getTruststore().getLocation(),
                frontendSslProperties.getTruststore().getFormat(),
                frontendSslProperties.getTruststore().getPassword()
        );
    }

    static HermesServerParameters createHermesServerParameters(FrontendSslProperties frontendSslProperties) {
        // todo add necessary properties classes and finish this constructor
        return new HermesServerParameters(
                null, // FRONTEND_TOPIC_METADATA_REFRESH_JOB_ENABLED
                null, //FRONTEND_GRACEFUL_SHUTDOWN_ENABLED
                null, // FRONTEND_GRACEFUL_SHUTDOWN_INITIAL_WAIT_MS
                null, // FRONTEND_REQUEST_PARSE_TIMEOUT
                null, // FRONTEND_MAX_HEADERS
                null, // FRONTEND_MAX_COOKIES
                null, // FRONTEND_MAX_PARAMETERS
                null, // FRONTEND_ALWAYS_SET_KEEP_ALIVE
                null, // FRONTEND_SET_KEEP_ALIVE
                null, // FRONTEND_BACKLOG_SIZE
                null, // FRONTEND_READ_TIMEOUT
                null, // FRONTEND_IO_THREADS_COUNT
                null, // FRONTEND_WORKER_THREADS_COUNT
                null, // FRONTEND_BUFFER_SIZE
                frontendSslProperties.isEnabled(),
                frontendSslProperties.getClientAuthMode(),
                null, // FRONTEND_HTTP2_ENABLED
                null, // FRONTEND_REQUEST_DUMPER
                null, // FRONTEND_PORT
                frontendSslProperties.getProt(),
                null // FRONTEND_HOST
        );
    }
}
