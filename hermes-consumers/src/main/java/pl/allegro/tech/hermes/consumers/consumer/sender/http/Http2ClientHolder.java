package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import java.util.Optional;
import org.eclipse.jetty.client.HttpClient;

public class Http2ClientHolder {

  private final HttpClient http2Client;

  public Http2ClientHolder(HttpClient http2Client) {
    this.http2Client = http2Client;
  }

  Optional<HttpClient> getHttp2Client() {
    return Optional.ofNullable(http2Client);
  }
}
