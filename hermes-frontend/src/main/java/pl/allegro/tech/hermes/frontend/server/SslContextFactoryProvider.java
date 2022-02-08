package pl.allegro.tech.hermes.frontend.server;

import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.ssl.*;
import pl.allegro.tech.hermes.common.ssl.jvm.JvmKeyManagersProvider;
import pl.allegro.tech.hermes.common.ssl.jvm.JvmTrustManagerProvider;
import pl.allegro.tech.hermes.common.ssl.provided.ProvidedKeyManagersProvider;
import pl.allegro.tech.hermes.common.ssl.provided.ProvidedTrustManagersProvider;

import javax.inject.Inject;
import java.util.Optional;

import static pl.allegro.tech.hermes.common.config.Configs.*;
import static pl.allegro.tech.hermes.common.ssl.KeystoreSource.JRE;
import static pl.allegro.tech.hermes.common.ssl.KeystoreSource.PROVIDED;

public class SslContextFactoryProvider {

//    @Inject
//    @org.jvnet.hk2.annotations.Optional
    SslContextFactory sslContextFactory;

//    @Inject
    ConfigFactory configFactory;

    public SslContextFactoryProvider(SslContextFactory sslContextFactory, ConfigFactory configFactory) {
        this.sslContextFactory = sslContextFactory;
        this.configFactory = configFactory;
    }

    public SslContextFactory getSslContextFactory() {
        return Optional.ofNullable(sslContextFactory).orElse(getDefault());
    }

    private SslContextFactory getDefault() {
        String protocol = configFactory.getStringProperty(Configs.FRONTEND_SSL_PROTOCOL);
        KeyManagersProvider keyManagersProvider = createKeyManagersProvider();
        TrustManagersProvider trustManagersProvider = createTrustManagersProvider();
        return new DefaultSslContextFactory(protocol, keyManagersProvider, trustManagersProvider);
    }

    private KeyManagersProvider createKeyManagersProvider() {
        String keystoreSource = configFactory.getStringProperty(FRONTEND_SSL_KEYSTORE_SOURCE);
        if (PROVIDED.getValue().equals(keystoreSource)) {
            KeystoreProperties properties = new KeystoreProperties(
                    configFactory.getStringProperty(FRONTEND_SSL_KEYSTORE_LOCATION),
                    configFactory.getStringProperty(FRONTEND_SSL_KEYSTORE_FORMAT),
                    configFactory.getStringProperty(FRONTEND_SSL_KEYSTORE_PASSWORD)
            );
            return new ProvidedKeyManagersProvider(properties);
        }
        if (JRE.getValue().equals(keystoreSource)) {
            return new JvmKeyManagersProvider();
        }
        throw new KeystoreConfigurationException(keystoreSource);
    }

    public TrustManagersProvider createTrustManagersProvider() {
        String truststoreSource = configFactory.getStringProperty(FRONTEND_SSL_TRUSTSTORE_SOURCE);
        if (PROVIDED.getValue().equals(truststoreSource)) {
            KeystoreProperties properties = new KeystoreProperties(
                    configFactory.getStringProperty(FRONTEND_SSL_TRUSTSTORE_LOCATION),
                    configFactory.getStringProperty(FRONTEND_SSL_TRUSTSTORE_FORMAT),
                    configFactory.getStringProperty(FRONTEND_SSL_TRUSTSTORE_PASSWORD)
            );
            return new ProvidedTrustManagersProvider(properties);
        }
        if (JRE.getValue().equals(truststoreSource)) {
            return new JvmTrustManagerProvider();
        }
        throw new TruststoreConfigurationException(truststoreSource);
    }
}
