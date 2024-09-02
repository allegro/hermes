package pl.allegro.tech.hermes.common.ssl;

import javax.net.ssl.SSLContext;

public class DefaultSslContextFactory implements SslContextFactory {

  private final String protocol;
  private final KeyManagersProvider keyManagersProvider;
  private final TrustManagersProvider trustManagersProvider;

  public DefaultSslContextFactory(
      String protocol,
      KeyManagersProvider keyManagersProvider,
      TrustManagersProvider trustManagersProvider) {
    this.protocol = protocol;
    this.keyManagersProvider = keyManagersProvider;
    this.trustManagersProvider = trustManagersProvider;
  }

  @Override
  public SSLContextHolder create() {
    try {
      SSLContext sslContext = SSLContext.getInstance(protocol);
      sslContext.init(
          keyManagersProvider.getKeyManagers(), trustManagersProvider.getTrustManagers(), null);
      return new SSLContextHolder(sslContext, trustManagersProvider.getTrustManagers());
    } catch (Exception e) {
      throw new SslContextCreationException(e);
    }
  }
}
