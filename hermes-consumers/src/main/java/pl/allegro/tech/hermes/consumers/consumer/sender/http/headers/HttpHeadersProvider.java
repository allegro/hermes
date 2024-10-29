package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers;

import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpRequestData;

public interface HttpHeadersProvider {

  HttpRequestHeaders getHeaders(Message message, HttpRequestData requestData);
}
