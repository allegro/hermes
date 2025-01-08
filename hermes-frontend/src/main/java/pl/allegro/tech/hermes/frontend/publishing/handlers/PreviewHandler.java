package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewLog;

public class PreviewHandler implements HttpHandler {

  private final HttpHandler next;
  private final MessagePreviewLog messagePreviewLog;

  public PreviewHandler(HttpHandler next, MessagePreviewLog messagePreviewLog) {
    this.next = next;
    this.messagePreviewLog = messagePreviewLog;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    AttachmentContent attachment = exchange.getAttachment(AttachmentContent.KEY);

    try {
      next.handleRequest(exchange);
    } finally {
      messagePreviewLog.add(attachment.getTopic(), attachment.getMessage());
    }
  }
}
