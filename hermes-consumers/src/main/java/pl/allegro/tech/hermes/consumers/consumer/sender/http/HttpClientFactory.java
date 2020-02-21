package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.api.Factory;

import javax.inject.Inject;

public class HttpClientFactory implements Factory<HttpClient> {

    private final HttpClientsFactory httpClientsFactory;

    @Inject
    public HttpClientFactory(HttpClientsFactory httpClientsFactory) {
        this.httpClientsFactory = httpClientsFactory;
    }

    @Override
    public HttpClient provide() {
        return httpClientsFactory.createClientForHttp1("jetty-http-client");
    }

    @Override
    public void dispose(HttpClient instance) {
    }
}
