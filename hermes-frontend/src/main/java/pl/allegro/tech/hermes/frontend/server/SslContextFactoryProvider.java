package pl.allegro.tech.hermes.frontend.server;

import pl.allegro.tech.hermes.common.config.ConfigFactory;

import javax.inject.Inject;
import java.util.Optional;

public class SslContextFactoryProvider {

    @Inject
    @org.jvnet.hk2.annotations.Optional
    SslContextFactory sslContextFactory;

    @Inject
    ConfigFactory configFactory;

    public SslContextFactory getSslContextFactory() {
        return Optional.ofNullable(sslContextFactory).orElse(new JvmKeystoreSslContextFactory(configFactory));
    }
}
