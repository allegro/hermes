package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.eclipse.jetty.http.HttpHeader;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpRequestData;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.HttpAuthorizationProvider;

public final class AuthHeadersProvider implements HttpHeadersProvider {

  private final HttpHeadersProvider headersProvider;
  private final HttpAuthorizationProvider authorizationProvider;

  public AuthHeadersProvider(
      HttpHeadersProvider headersProvider, HttpAuthorizationProvider authorizationProvider) {
    this.headersProvider = headersProvider;
    this.authorizationProvider = authorizationProvider;
  }

  @Override
  public HttpRequestHeaders getHeaders(Message message, HttpRequestData requestData) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

    if (headersProvider != null) {
      builder.putAll(headersProvider.getHeaders(message, requestData).asMap());
    }

    if (authorizationProvider != null) {
      Optional<String> token = authorizationProvider.authorizationToken();
      token.ifPresent(s -> builder.put(HttpHeader.AUTHORIZATION.toString(), s));
    }

    return new HttpRequestHeaders(builder.build());
  }
}
