package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import io.undertow.UndertowLogger;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import java.io.IOException;
import org.xnio.IoUtils;
import pl.allegro.tech.hermes.frontend.publishing.handlers.AttachmentContent;

/*
   Marks the response as ready and ends the exchange
*/
class ResponseReadyIoCallback implements IoCallback {

  static final IoCallback INSTANCE = new ResponseReadyIoCallback();

  private static final IoCallback CALLBACK =
      new IoCallback() {
        @Override
        public void onComplete(final HttpServerExchange exchange, final Sender sender) {
          exchange.endExchange();
        }

        @Override
        public void onException(
            final HttpServerExchange exchange, final Sender sender, final IOException exception) {
          UndertowLogger.REQUEST_IO_LOGGER.ioException(exception);
          exchange.endExchange();
        }
      };

  @Override
  public void onComplete(final HttpServerExchange exchange, final Sender sender) {
    markResponseAsReady(exchange);
    sender.close(CALLBACK);
  }

  @Override
  public void onException(
      final HttpServerExchange exchange, final Sender sender, final IOException exception) {
    try {
      markResponseAsReady(exchange);
      exchange.endExchange();
    } finally {
      IoUtils.safeClose(exchange.getConnection());
    }
  }

  private void markResponseAsReady(HttpServerExchange exchange) {
    AttachmentContent attachment = exchange.getAttachment(AttachmentContent.KEY);
    if (attachment != null) {
      attachment.markResponseAsReady();
    }
  }
}
