package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import pl.allegro.tech.hermes.consumers.config.SslContextProperties;

import java.util.Optional;

import static pl.allegro.tech.hermes.common.ssl.KeystoreSource.JRE;
import static pl.allegro.tech.hermes.common.ssl.KeystoreSource.PROVIDED;

public class SslContextFactoryProvider {

    private final SslContextFactory sslContextFactory;

    private final SslContextProperties sslContextProperties;

    public SslContextFactoryProvider(SslContextFactory sslContextFactory, SslContextProperties sslContextProperties) {
        this.sslContextFactory = sslContextFactory;
        this.sslContextProperties = sslContextProperties;
    }

    public Optional<org.eclipse.jetty.util.ssl.SslContextFactory> provideSslContextFactory() {
        if (sslContextProperties.isEnabled()) {
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
        String protocol = sslContextProperties.getProtocol();
        KeyManagersProvider keyManagersProvider = createKeyManagersProvider();
        TrustManagersProvider trustManagersProvider = createTrustManagersProvider();
        return new DefaultSslContextFactory(protocol, keyManagersProvider, trustManagersProvider);
    }

    private KeyManagersProvider createKeyManagersProvider() {
        String keystoreSource = sslContextProperties.getKeystoreSource();
        if (PROVIDED.getValue().equals(keystoreSource)) {
            KeystoreProperties properties = new KeystoreProperties(
                    sslContextProperties.getKeystoreLocation(),
                    sslContextProperties.getKeystoreFormat(),
                    sslContextProperties.getKeystorePassword()
            );
            return new ProvidedKeyManagersProvider(properties);
        }
        if (JRE.getValue().equals(keystoreSource)) {
            return new JvmKeyManagersProvider();
        }
        throw new KeystoreConfigurationException(keystoreSource);
    }

    public TrustManagersProvider createTrustManagersProvider() {
        String truststoreSource = sslContextProperties.getTruststoreSource();
        if (PROVIDED.getValue().equals(truststoreSource)) {
            KeystoreProperties properties = new KeystoreProperties(
                    sslContextProperties.getKeystoreLocation(),
                    sslContextProperties.getKeystoreFormat(),
                    sslContextProperties.getKeystorePassword()
            );
            return new ProvidedTrustManagersProvider(properties);
        }
        if (JRE.getValue().equals(truststoreSource)) {
            return new JvmTrustManagerProvider();
        }
        throw new TruststoreConfigurationException(truststoreSource);
    }
}
