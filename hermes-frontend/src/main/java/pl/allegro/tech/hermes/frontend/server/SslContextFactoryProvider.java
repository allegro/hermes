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
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_SSL_TRUSTSTORE_DEFAULT_JVM;

public class SslContextFactoryProvider {

    @Inject
    @org.jvnet.hk2.annotations.Optional
    SslContextFactory sslContextFactory;

    @Inject
    ConfigFactory configFactory;

    public SslContextFactory getSslContextFactory() {
        return Optional.ofNullable(sslContextFactory).orElse(getDefault());
    }

    private SslContextFactory getDefault() {
        String protocol = configFactory.getStringProperty(Configs.FRONTEND_SSL_PROTOCOL);
        KeyManagersProvider keyManagersProvider = createKeyManagersProvider();
        TrustManagersProvider trustManagersProvider = createTrustManagersProvider();
        return new DefaultSslContextFactory(protocol, keyManagersProvider, trustManagersProvider);
    }

    public KeyManagersProvider createKeyManagersProvider() {
        if (configFactory.getBooleanProperty(FRONTEND_SSL_KEYSTORE_PROVIDED)) {
            KeystoreProperties properties = new KeystoreProperties(
                    configFactory.getStringProperty(FRONTEND_SSL_KEYSTORE_LOCATION),
                    configFactory.getStringProperty(FRONTEND_SSL_KEYSTORE_FORMAT),
                    configFactory.getStringProperty(FRONTEND_SSL_KEYSTORE_PASSWORD)
            );
            return new ProvidedKeyManagersProvider(properties);
        } else if (configFactory.getBooleanProperty(FRONTEND_SSL_KEYSTORE_DEFAULT_JVM)) {
            return new JvmKeyManagersProvider();
        } else {
            throw new KeyStoreConfigurationException();
        }
    }

    public TrustManagersProvider createTrustManagersProvider() {
        if (configFactory.getBooleanProperty(FRONTEND_SSL_TRUSTSTORE_PROVIDED)) {
            KeystoreProperties properties = new KeystoreProperties(
                    configFactory.getStringProperty(FRONTEND_SSL_TRUSTSTORE_LOCATION),
                    configFactory.getStringProperty(FRONTEND_SSL_TRUSTSTORE_FORMAT),
                    configFactory.getStringProperty(FRONTEND_SSL_TRUSTSTORE_PASSWORD)
            );
            return new ProvidedTrustManagersProvider(properties);
        } else if (configFactory.getBooleanProperty(FRONTEND_SSL_TRUSTSTORE_DEFAULT_JVM)) {
            return new JvmTrustManagerProvider();
        } else {
            throw new TrustStoreConfigurationException();
        }
    }
}
