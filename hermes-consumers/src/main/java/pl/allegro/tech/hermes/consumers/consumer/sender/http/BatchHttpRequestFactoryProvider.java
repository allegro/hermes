package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import pl.allegro.tech.hermes.api.Subscription;

public interface BatchHttpRequestFactoryProvider {
    BatchHttpRequestFactory provideRequestFactory(Subscription subscription, HttpClient httpClient);
}
