package pl.allegro.tech.hermes.consumers.consumer.sender.http;

public interface Http1ClientParameters extends HttpClientParameters {

  int getMaxConnectionsPerDestination();
}
