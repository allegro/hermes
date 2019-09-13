package pl.allegro.tech.hermes.consumers.consumer.oauth.client;

import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientsFactory;

import javax.inject.Inject;

public class OAuthHttpClientFactory implements Factory<HttpClient> {

    private final HttpClientsFactory httpClientsFactory;

    @Inject
    public OAuthHttpClientFactory(HttpClientsFactory httpClientsFactory) {
        this.httpClientsFactory = httpClientsFactory;
    }

    @Override
    public HttpClient provide() {
        return httpClientsFactory.createClientForHttp1("jetty-http-oauthclient");
    }

    @Override
    public void dispose(HttpClient instance) {
    }
}
