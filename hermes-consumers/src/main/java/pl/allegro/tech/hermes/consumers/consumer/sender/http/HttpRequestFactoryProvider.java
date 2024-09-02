package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

public interface HttpRequestFactoryProvider {
  HttpRequestFactory provideRequestFactory(
      Subscription subscription, HttpClient httpClient, MetadataAppender<Request> metadataAppender);
}
