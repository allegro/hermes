package pl.allegro.tech.hermes.consumers.consumer.sender.http;

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

  private final SslContextParameters sslContextParams;

  public SslContextFactoryProvider(
      SslContextFactory sslContextFactory, SslContextParameters sslContextParams) {
    this.sslContextFactory = sslContextFactory;
    this.sslContextParams = sslContextParams;
  }

  public Optional<org.eclipse.jetty.util.ssl.SslContextFactory.Client> provideSslContextFactory() {
    if (sslContextParams.isEnabled()) {
      org.eclipse.jetty.util.ssl.SslContextFactory.Client sslCtx =
          new org.eclipse.jetty.util.ssl.SslContextFactory.Client();
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
    String protocol = sslContextParams.getProtocol();
    KeyManagersProvider keyManagersProvider = createKeyManagersProvider();
    TrustManagersProvider trustManagersProvider = createTrustManagersProvider();
    return new DefaultSslContextFactory(protocol, keyManagersProvider, trustManagersProvider);
  }

  private KeyManagersProvider createKeyManagersProvider() {
    String keystoreSource = sslContextParams.getKeystoreSource();
    if (PROVIDED.getValue().equals(keystoreSource)) {
      KeystoreProperties properties =
          new KeystoreProperties(
              sslContextParams.getKeystoreLocation(),
              sslContextParams.getKeystoreFormat(),
              sslContextParams.getKeystorePassword());
      return new ProvidedKeyManagersProvider(properties);
    }
    if (JRE.getValue().equals(keystoreSource)) {
      return new JvmKeyManagersProvider();
    }
    throw new KeystoreConfigurationException(keystoreSource);
  }

  public TrustManagersProvider createTrustManagersProvider() {
    String truststoreSource = sslContextParams.getTruststoreSource();
    if (PROVIDED.getValue().equals(truststoreSource)) {
      KeystoreProperties properties =
          new KeystoreProperties(
              sslContextParams.getTruststoreLocation(),
              sslContextParams.getTruststoreFormat(),
              sslContextParams.getTruststorePassword());
      return new ProvidedTrustManagersProvider(properties);
    }
    if (JRE.getValue().equals(truststoreSource)) {
      return new JvmTrustManagerProvider();
    }
    throw new TruststoreConfigurationException(truststoreSource);
  }
}
