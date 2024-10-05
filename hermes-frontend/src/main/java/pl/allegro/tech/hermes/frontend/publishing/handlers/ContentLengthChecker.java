package pl.allegro.tech.hermes.frontend.publishing.handlers;

import static java.lang.String.format;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ContentLengthChecker {
  private static final Logger logger = LoggerFactory.getLogger(ContentLengthChecker.class);

  private final boolean forceMaxMessageSizePerTopic;

  ContentLengthChecker(boolean forceMaxMessageSizePerTopic) {
    this.forceMaxMessageSizePerTopic = forceMaxMessageSizePerTopic;
  }

  void check(HttpServerExchange exchange, int contentLength, AttachmentContent attachment)
      throws InvalidContentLengthException, ContentTooLargeException {

    int max = attachment.getCachedTopic().getTopic().getMaxMessageSize();
    long expected = exchange.getRequestContentLength();
    if (expected != contentLength && !isChunked(exchange.getRequestHeaders(), expected)) {
      throw new InvalidContentLengthException(expected, contentLength);
    } else if (contentLength > max) {
      if (forceMaxMessageSizePerTopic) {
        throw new ContentTooLargeException(contentLength, max);
      } else {
        logger.warn(
            "Content-Length is larger than max on this topic [length:{}, max:{}, topic: {}]",
            contentLength,
            max,
            attachment.getCachedTopic().getQualifiedName());
      }
    }
  }

  private static boolean isChunked(HeaderMap headerMap, long requestContentLength) {
    HeaderValues headerValue = headerMap.get(Headers.TRANSFER_ENCODING);
    return headerValue != null
        && ("chunked".equals(headerValue.getFirst()) && requestContentLength < 0);
  }

  public static final class InvalidContentLengthException extends IOException {
    InvalidContentLengthException(long expected, int contentLength) {
      super(
          format(
              "Content-Length does not match the header [header:%s, actual:%s].",
              expected, contentLength));
    }
  }

  public static final class ContentTooLargeException extends IOException {
    ContentTooLargeException(long contentLength, int max) {
      super(
          format(
              "Content-Length is larger than max on this topic [length:%s, max:%s].",
              contentLength, max));
    }
  }
}
