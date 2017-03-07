package pl.allegro.tech.hermes.frontend.server;

import javax.net.ssl.SSLContext;

public interface SslContextFactory {

    SSLContext create();
}
