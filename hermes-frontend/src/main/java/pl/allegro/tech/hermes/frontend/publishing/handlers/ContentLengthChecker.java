package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;

import java.io.IOException;

import static java.lang.String.format;

final class ContentLengthChecker {

    static void checkContentLength(HttpServerExchange exchange, int contentLength, int max)
            throws InvalidContentLengthException, ContentTooLargeException {

        long expected = exchange.getRequestContentLength();
        if (expected != contentLength && !isChunked(exchange.getRequestHeaders(), expected)) {
            throw new InvalidContentLengthException(expected, contentLength);
        } else if (contentLength > max) {
            throw new ContentTooLargeException(expected, max);
        }
    }

    private static boolean isChunked(HeaderMap headerMap, long requestContentLength) {
        HeaderValues headerValue = headerMap.get(Headers.TRANSFER_ENCODING);
        return headerValue == null ? false : "chunked".equals(headerValue.getFirst()) && requestContentLength < 0;
    }

    public static final class InvalidContentLengthException extends IOException {
        InvalidContentLengthException(long expected, int contentLength) {
            super(format("Content-Length does not match the header [header:%s, actual:%s].",
                    expected, contentLength));
        }
    }

    public static final class ContentTooLargeException extends IOException {
        ContentTooLargeException(long contentLength, int max) {
            super(format("Content-Length is larger than max on this topic [length:%s, max:%s].",
                    contentLength, max));
        }
    }
}
