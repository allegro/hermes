package pl.allegro.tech.hermes.common.ssl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

public final class SSLContextHolder {

  private final SSLContext sslContext;
  private final TrustManager[] trustManagers;

  public SSLContextHolder(SSLContext sslContext, TrustManager[] trustManagers) {
    this.sslContext = sslContext;
    this.trustManagers = trustManagers;
  }

  public SSLContext getSslContext() {
    return sslContext;
  }

  public TrustManager[] getTrustManagers() {
    return trustManagers;
  }
}
