package pl.allegro.tech.hermes.common.di.factories;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.base.Optional;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.net.URI;

public class GraphiteWebTargetFactory implements Factory<WebTarget> {

    private URI graphiteURI;
    private Optional<Client> client;

    @Inject
    public GraphiteWebTargetFactory(ConfigFactory configFactory) {
        graphiteURI = URI.create(String.format("http://%s:%d",
            configFactory.getStringProperty(Configs.GRAPHITE_HOST),
            configFactory.getIntProperty(Configs.GRAPHITE_HTTP_PORT)
        ));

        client = Optional.absent();
    }

    @Override
    public WebTarget provide() {
        if (!client.isPresent()) {
            client = Optional.of(ClientBuilder.newClient());
            client.get().register(JacksonJsonProvider.class);
        }

        return client.get().target(graphiteURI);
    }

    @Override
    public void dispose(WebTarget instance) {
        if (client.isPresent()) {
            client.get().close();
        }
    }
}
