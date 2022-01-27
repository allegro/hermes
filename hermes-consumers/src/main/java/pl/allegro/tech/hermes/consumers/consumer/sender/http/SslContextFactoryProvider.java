package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.ssl.DefaultSslContextFactory;
import pl.allegro.tech.hermes.common.ssl.KeyManagersProvider;
import pl.allegro.tech.hermes.common.ssl.KeystoreConfigurationException;
import pl.allegro.tech.hermes.common.ssl.KeystoreProperties;
import pl.allegro.tech.hermes.common.ssl.SslContextFactory;
import pl.allegro.tech.hermes.common.ssl.TrustManagersProvider;
import pl.allegro.tech.hermes.common.ssl.TruststoreConfigurationException;
import pl.allegro.tech.hermes.common.ssl.jvm.JvmKeyManagersProvider;
import pl.allegro.tech.hermes.common.ssl.jvm.JvmTrustManagerProvider;
import pl.allegro.tech.hermes.common.ssl.provided.ProvidedKeyManagersProvider;
import pl.allegro.tech.hermes.common.ssl.provided.ProvidedTrustManagersProvider;

import java.util.Optional;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SSL_KEYSTORE_FORMAT;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SSL_KEYSTORE_LOCATION;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SSL_KEYSTORE_PASSWORD;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SSL_KEYSTORE_SOURCE;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SSL_TRUSTSTORE_FORMAT;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SSL_TRUSTSTORE_LOCATION;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SSL_TRUSTSTORE_PASSWORD;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SSL_TRUSTSTORE_SOURCE;
import static pl.allegro.tech.hermes.common.ssl.KeystoreSource.JRE;
import static pl.allegro.tech.hermes.common.ssl.KeystoreSource.PROVIDED;

public class SslContextFactoryProvider {

    private final SslContextFactory sslContextFactory;
    private final ConfigFactory configFactory;

    public SslContextFactoryProvider(SslContextFactory sslContextFactory, ConfigFactory configFactory) {
        this.sslContextFactory = sslContextFactory;
        this.configFactory = configFactory;
    }

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
        String keystoreSource = configFactory.getStringProperty(CONSUMER_SSL_KEYSTORE_SOURCE);
        if (PROVIDED.getValue().equals(keystoreSource)) {
            KeystoreProperties properties = new KeystoreProperties(
                    configFactory.getStringProperty(CONSUMER_SSL_KEYSTORE_LOCATION),
                    configFactory.getStringProperty(CONSUMER_SSL_KEYSTORE_FORMAT),
                    configFactory.getStringProperty(CONSUMER_SSL_KEYSTORE_PASSWORD)
            );
            return new ProvidedKeyManagersProvider(properties);
        }
        if (JRE.getValue().equals(keystoreSource)) {
            return new JvmKeyManagersProvider();
        }
        throw new KeystoreConfigurationException(keystoreSource);
    }

    public TrustManagersProvider createTrustManagersProvider() {
        String truststoreSource = configFactory.getStringProperty(CONSUMER_SSL_TRUSTSTORE_SOURCE);
        if (PROVIDED.getValue().equals(truststoreSource)) {
            KeystoreProperties properties = new KeystoreProperties(
                    configFactory.getStringProperty(CONSUMER_SSL_TRUSTSTORE_LOCATION),
                    configFactory.getStringProperty(CONSUMER_SSL_TRUSTSTORE_FORMAT),
                    configFactory.getStringProperty(CONSUMER_SSL_TRUSTSTORE_PASSWORD)
            );
            return new ProvidedTrustManagersProvider(properties);
        }
        if (JRE.getValue().equals(truststoreSource)) {
            return new JvmTrustManagerProvider();
        }
        throw new TruststoreConfigurationException(truststoreSource);
    }
}
