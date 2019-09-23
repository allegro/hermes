package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers;

import com.google.common.collect.ImmutableMap;
import org.eclipse.jetty.http.HttpHeader;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.util.function.Function;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.AvroMediaType.AVRO_BINARY;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;

public final class Http2HeadersProvider implements HttpHeadersProvider {

    private static final Function<ContentType, String> contentTypeToMediaType = contentType ->
            AVRO.equals(contentType) ? AVRO_BINARY : APPLICATION_JSON;

    @Override
    public HttpRequestHeaders getHeaders(Message message) {
        return new HttpRequestHeaders(
                ImmutableMap.of(HttpHeader.CONTENT_TYPE.toString(), contentTypeToMediaType.apply(message.getContentType()))
        );
    }

}
