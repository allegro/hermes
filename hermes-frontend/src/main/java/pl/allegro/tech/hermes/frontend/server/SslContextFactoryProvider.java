package pl.allegro.tech.hermes.frontend.server;

import static pl.allegro.tech.hermes.common.ssl.KeystoreSource.JRE;
import static pl.allegro.tech.hermes.common.ssl.KeystoreSource.PROVIDED;

import java.util.Optional;
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

public class SslContextFactoryProvider {

  private final SslContextFactory sslContextFactory;
  private final SslParameters sslParameters;

  public SslContextFactoryProvider(
      SslContextFactory sslContextFactory, SslParameters sslParameters) {
    this.sslContextFactory = sslContextFactory;
    this.sslParameters = sslParameters;
  }

  public SslContextFactory getSslContextFactory() {
    return Optional.ofNullable(sslContextFactory).orElse(getDefault());
  }

  private SslContextFactory getDefault() {
    String protocol = sslParameters.getProtocol();
    KeyManagersProvider keyManagersProvider = createKeyManagersProvider();
    TrustManagersProvider trustManagersProvider = createTrustManagersProvider();
    return new DefaultSslContextFactory(protocol, keyManagersProvider, trustManagersProvider);
  }

  private KeyManagersProvider createKeyManagersProvider() {
    String keystoreSource = sslParameters.getKeystoreSource();
    if (PROVIDED.getValue().equals(keystoreSource)) {
      KeystoreProperties properties =
          new KeystoreProperties(
              sslParameters.getKeystoreLocation(),
              sslParameters.getKeystoreFormat(),
              sslParameters.getKeystorePassword());
      return new ProvidedKeyManagersProvider(properties);
    }
    if (JRE.getValue().equals(keystoreSource)) {
      return new JvmKeyManagersProvider();
    }
    throw new KeystoreConfigurationException(keystoreSource);
  }

  public TrustManagersProvider createTrustManagersProvider() {
    String truststoreSource = sslParameters.getTruststoreSource();
    if (PROVIDED.getValue().equals(truststoreSource)) {
      KeystoreProperties properties =
          new KeystoreProperties(
              sslParameters.getTruststoreLocation(),
              sslParameters.getTruststoreFormat(),
              sslParameters.getTruststorePassword());
      return new ProvidedTrustManagersProvider(properties);
    }
    if (JRE.getValue().equals(truststoreSource)) {
      return new JvmTrustManagerProvider();
    }
    throw new TruststoreConfigurationException(truststoreSource);
  }
}
