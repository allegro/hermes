package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;

import java.util.Optional;

public class Http2ClientHolder {

    private final HttpClient http2Client;

    Http2ClientHolder(HttpClient http2Client) {
        this.http2Client = http2Client;
    }

    Optional<HttpClient> getHttp2Client() {
        return Optional.ofNullable(http2Client);
    }
}
