package pl.allegro.tech.hermes.frontend.publishing.metadata;

import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;

import java.util.Map;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

public class HeadersToMapTransformer {
    public static Map<String, String> toHeadersMap(HeaderMap headerMap) {
        return stream(spliteratorUnknownSize(headerMap.iterator(), 0), false)
                .collect(toMap(
                        h -> h.getHeaderName().toString(),
                        HeaderValues::getFirst));
    }
}
