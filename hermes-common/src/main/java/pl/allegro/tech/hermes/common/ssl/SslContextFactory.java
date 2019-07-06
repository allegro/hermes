package pl.allegro.tech.hermes.common.ssl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

public interface SslContextFactory {

    SSLContext create();

    TrustManager[] getTrustManagers();
}
