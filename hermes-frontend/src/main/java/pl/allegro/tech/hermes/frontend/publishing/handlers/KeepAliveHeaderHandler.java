package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.SetHeaderHandler;

class KeepAliveHeaderHandler extends SetHeaderHandler {

  KeepAliveHeaderHandler(HttpHandler next, int keepAliveTimeoutSec) {
    super(next, "Keep-Alive", "timeout=" + keepAliveTimeoutSec);
  }
}
