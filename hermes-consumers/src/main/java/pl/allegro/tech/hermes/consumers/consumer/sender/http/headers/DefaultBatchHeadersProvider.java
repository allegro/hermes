package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers;

import java.util.Collections;
import pl.allegro.tech.hermes.api.EndpointAddress;

public final class DefaultBatchHeadersProvider implements BatchHttpHeadersProvider {

  @Override
  public HttpRequestHeaders getHeaders(EndpointAddress address) {
    return new HttpRequestHeaders(Collections.emptyMap());
  }
}
