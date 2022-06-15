package pl.allegro.tech.hermes.frontend.server;

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

import static pl.allegro.tech.hermes.common.ssl.KeystoreSource.JRE;
import static pl.allegro.tech.hermes.common.ssl.KeystoreSource.PROVIDED;

public class SslContextFactoryProvider {

    private final SslContextFactory sslContextFactory;
    private final ContextFactoryParameters contextFactoryParameters;

    public SslContextFactoryProvider(SslContextFactory sslContextFactory, ContextFactoryParameters contextFactoryParameters) {
        this.sslContextFactory = sslContextFactory;
        this.contextFactoryParameters = contextFactoryParameters;
    }

    public SslContextFactory getSslContextFactory() {
        return Optional.ofNullable(sslContextFactory).orElse(getDefault());
    }

    private SslContextFactory getDefault() {
        KeyManagersProvider keyManagersProvider = createKeyManagersProvider();
        TrustManagersProvider trustManagersProvider = createTrustManagersProvider();
        return new DefaultSslContextFactory(contextFactoryParameters.getFrontendSslProtocol(), keyManagersProvider, trustManagersProvider);
    }

    private KeyManagersProvider createKeyManagersProvider() {
        String keystoreSource = contextFactoryParameters.getSslKeystoreSource();

        if (PROVIDED.getValue().equals(keystoreSource)) {
            KeystoreProperties properties = new KeystoreProperties(
                    contextFactoryParameters.getSslKeystoreLocation(),
                    contextFactoryParameters.getSslKeystoreFormat(),
                    contextFactoryParameters.getSslKeystorePassword()
            );
            return new ProvidedKeyManagersProvider(properties);
        }
        if (JRE.getValue().equals(keystoreSource)) {
            return new JvmKeyManagersProvider();
        }
        throw new KeystoreConfigurationException(keystoreSource);
    }

    public TrustManagersProvider createTrustManagersProvider() {
        String truststoreSource = contextFactoryParameters.getSslTrustStoreSource();
        if (PROVIDED.getValue().equals(truststoreSource)) {
            KeystoreProperties properties = new KeystoreProperties(
                    contextFactoryParameters.getSslTrustStoreLocation(),
                    contextFactoryParameters.getSslTrustStoreFormat(),
                    contextFactoryParameters.getSslTrustStorePassword()
            );
            return new ProvidedTrustManagersProvider(properties);
        }
        if (JRE.getValue().equals(truststoreSource)) {
            return new JvmTrustManagerProvider();
        }
        throw new TruststoreConfigurationException(truststoreSource);
    }
}
