package pl.allegro.tech.hermes.frontend.publishing.metadata;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

import com.google.common.collect.ImmutableMap;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import pl.allegro.tech.hermes.frontend.config.HTTPHeadersProperties;
import pl.allegro.tech.hermes.frontend.publishing.handlers.end.TrackingHeadersExtractor;

public class DefaultHeadersPropagator implements HeadersPropagator, TrackingHeadersExtractor {

  private final boolean propagate;
  private final Set<String> supportedHeaders;
  private final Set<String> trackingHeaders;

  public DefaultHeadersPropagator(HTTPHeadersProperties httpHeadersProperties) {
    propagate = httpHeadersProperties.isPropagationEnabled();
    supportedHeaders = httpHeadersProperties.getAllowedSet();
    trackingHeaders =
        new HashSet<>() {
          {
            addAll(httpHeadersProperties.getAllowedSet());
            addAll(httpHeadersProperties.getAdditionalAllowedSetToLog());
          }
        };
  }

  @Override
  public Map<String, String> extract(HeaderMap headerMap) {
    if (propagate) {
      Map<String, String> headers = toHeadersMap(headerMap);
      if (supportedHeaders.isEmpty()) {
        return ImmutableMap.copyOf(headers);
      }

      return extractHeaders(headers, supportedHeaders);
    } else {
      return ImmutableMap.of();
    }
  }

  @Override
  public Map<String, String> extractHeadersToLog(HeaderMap headers) {
    return extractHeaders(toHeadersMap(headers), trackingHeaders);
  }

  private static Map<String, String> toHeadersMap(HeaderMap headerMap) {
    return stream(spliteratorUnknownSize(headerMap.iterator(), 0), false)
        .collect(toMap(h -> h.getHeaderName().toString(), HeaderValues::getFirst));
  }

  private static Map<String, String> extractHeaders(
      Map<String, String> headers, Set<String> headersToExtract) {
    return headers.entrySet().stream()
        .filter(headerEntry -> headersToExtract.contains(headerEntry.getKey().toLowerCase()))
        .filter(headerEntry -> !headerEntry.getValue().isEmpty())
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
