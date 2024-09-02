package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

public final class HttpRequestHeaders {

  private final Map<String, String> headers;

  public HttpRequestHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public Map<String, String> asMap() {
    return ImmutableMap.copyOf(headers);
  }
}
