package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.ssl.DefaultSslContextFactory;
import pl.allegro.tech.hermes.common.ssl.KeyManagersProvider;
import pl.allegro.tech.hermes.common.ssl.SslContextFactory;
import pl.allegro.tech.hermes.common.ssl.TrustManagersProvider;
import pl.allegro.tech.hermes.common.ssl.KeystoreProperties;
import pl.allegro.tech.hermes.common.ssl.KeyStoreConfigurationException;
import pl.allegro.tech.hermes.common.ssl.TrustStoreConfigurationException;
import pl.allegro.tech.hermes.common.ssl.jvm.JvmKeyManagersProvider;
import pl.allegro.tech.hermes.common.ssl.jvm.JvmTrustManagerProvider;
import pl.allegro.tech.hermes.common.ssl.provided.ProvidedKeyManagersProvider;
import pl.allegro.tech.hermes.common.ssl.provided.ProvidedTrustManagersProvider;

import javax.inject.Inject;
import java.util.Optional;

import static pl.allegro.tech.hermes.common.config.Configs.*;

public class SslContextFactoryProvider {

    @Inject
    @org.jvnet.hk2.annotations.Optional
    SslContextFactory sslContextFactory;

    @Inject
    ConfigFactory configFactory;

    public Optional<org.eclipse.jetty.util.ssl.SslContextFactory> provideSslContextFactory() {
        if (configFactory.getBooleanProperty(Configs.CONSUMER_SSL_ENABLED)) {
            org.eclipse.jetty.util.ssl.SslContextFactory sslCtx = new org.eclipse.jetty.util.ssl.SslContextFactory();
            sslCtx.setEndpointIdentificationAlgorithm("HTTPS");
            sslCtx.setSslContext(sslContextFactory().create().getSslContext());
            return Optional.of(sslCtx);
        } else {
            return Optional.empty();
        }
    }

    private SslContextFactory sslContextFactory() {
        return Optional.ofNullable(sslContextFactory).orElseGet(this::defaultSslContextFactory);
    }

    private SslContextFactory defaultSslContextFactory() {
        String protocol = configFactory.getStringProperty(Configs.CONSUMER_SSL_PROTOCOL);
        KeyManagersProvider keyManagersProvider = createKeyManagersProvider();
        TrustManagersProvider trustManagersProvider = createTrustManagersProvider();
        return new DefaultSslContextFactory(protocol, keyManagersProvider, trustManagersProvider);
    }

    private KeyManagersProvider createKeyManagersProvider() {
        if (configFactory.getBooleanProperty(CONSUMER_SSL_KEYSTORE_PROVIDED)) {
            KeystoreProperties properties = new KeystoreProperties(
                    configFactory.getStringProperty(CONSUMER_SSL_KEYSTORE_LOCATION),
                    configFactory.getStringProperty(CONSUMER_SSL_KEYSTORE_FORMAT),
                    configFactory.getStringProperty(CONSUMER_SSL_KEYSTORE_PASSWORD)
            );
            return new ProvidedKeyManagersProvider(properties);
        } else if (configFactory.getBooleanProperty(CONSUMER_SSL_KEYSTORE_DEFAULT_JVM)) {
            return new JvmKeyManagersProvider();
        } else {
            throw new KeyStoreConfigurationException();
        }
    }

    public TrustManagersProvider createTrustManagersProvider() {
        if (configFactory.getBooleanProperty(CONSUMER_SSL_TRUSTSTORE_PROVIDED)) {
            KeystoreProperties properties = new KeystoreProperties(
                    configFactory.getStringProperty(CONSUMER_SSL_TRUSTSTORE_LOCATION),
                    configFactory.getStringProperty(CONSUMER_SSL_TRUSTSTORE_FORMAT),
                    configFactory.getStringProperty(CONSUMER_SSL_TRUSTSTORE_PASSWORD)
            );
            return new ProvidedTrustManagersProvider(properties);
        } else if (configFactory.getBooleanProperty(CONSUMER_SSL_TRUSTSTORE_DEFAULT_JVM)) {
            return new JvmTrustManagerProvider();
        } else {
            throw new TrustStoreConfigurationException();
        }
    }
}
