package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.api.Factory;

import javax.inject.Inject;

public class Http2ClientFactory implements Factory<HttpClient> {

    private final HttpClientsFactory httpClientsFactory;

    @Inject
    public Http2ClientFactory(HttpClientsFactory httpClientsFactory) {
        this.httpClientsFactory = httpClientsFactory;
    }

    @Override
    public HttpClient provide() {
        return httpClientsFactory.createClientForHttp2();
    }

    @Override
    public void dispose(HttpClient instance) {
    }
}
