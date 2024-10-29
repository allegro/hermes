package pl.allegro.tech.hermes.frontend.publishing.handlers.end;

import io.undertow.server.HttpServerExchange;
import java.net.InetSocketAddress;

class RemoteHostReader {

  static String readHostAndPort(HttpServerExchange exchange) {
    InetSocketAddress sourceAddress = exchange.getSourceAddress();
    if (sourceAddress == null) {
      return "";
    }

    return sourceAddress.getHostString() + ":" + sourceAddress.getPort();
  }
}
