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

    public static HermesServerParameters createHermesServerParameters(FrontendSslProperties frontendSslProperties, FrontendBaseProperties frontendBaseProperties, TopicLoadingProperties topicLoadingProperties) {
        return new HermesServerParameters(
                topicLoadingProperties.getMetadataRefreshJob().isEnabled(),
                frontendBaseProperties.isGracefulShutdownEnabled(),
                frontendBaseProperties.getGracefulShutdownInitialWaitMs(),
                frontendBaseProperties.getRequestParseTimeout(),
                frontendBaseProperties.getMaxHeaders(),
                frontendBaseProperties.getMaxCookies(),
                frontendBaseProperties.getMaxParameters(),
                frontendBaseProperties.isAlwaysKeepAlive(),
                frontendBaseProperties.isKeepAlive(),
                frontendBaseProperties.getBacklogSize(),
                frontendBaseProperties.getReadTimeout(),
                frontendBaseProperties.getIoThreadsCount(),
                frontendBaseProperties.getWorkerThreadCount(),
                frontendBaseProperties.getBufferSize(),
                frontendSslProperties.isEnabled(),
                frontendSslProperties.getClientAuthMode(),
                frontendBaseProperties.isHttp2Enabled(),
                frontendBaseProperties.isRequestDumper(),
                frontendBaseProperties.getPort(),
                frontendSslProperties.getProt(),
                frontendBaseProperties.getHost()
        );
    }
}
