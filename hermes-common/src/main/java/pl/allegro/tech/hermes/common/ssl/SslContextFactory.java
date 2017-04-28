package pl.allegro.tech.hermes.common.ssl;

import javax.net.ssl.SSLContext;

public interface SslContextFactory {

    SSLContext create();
}
