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
        return new HermesServerParameters();
    }
}
