package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;

import javax.inject.Inject;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP2_ENABLED;

public class Http2ClientFactory implements Factory<Http2ClientHolder> {

    private final HttpClientsFactory httpClientsFactory;
    private final ConfigFactory configFactory;

    @Inject
    public Http2ClientFactory(HttpClientsFactory httpClientsFactory, ConfigFactory configFactory) {
        this.httpClientsFactory = httpClientsFactory;
        this.configFactory = configFactory;
    }

    @Override
    public Http2ClientHolder provide() {
        if (!configFactory.getBooleanProperty(CONSUMER_HTTP2_ENABLED)) {
            return new Http2ClientHolder(null);
        } else {
            return new Http2ClientHolder(httpClientsFactory.createClientForHttp2());
        }
    }

    @Override
    public void dispose(Http2ClientHolder instance) {
    }
}
