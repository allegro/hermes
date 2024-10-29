package pl.allegro.tech.hermes.management.domain.health;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class NodeDataProvider {

  private final String serverPort;

  NodeDataProvider(@Value("${server.port}") String serverPort) {
    this.serverPort = serverPort;
  }

  String getHostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      throw new CouldNotResolveHostNameException(e);
    }
  }

  String getServerPort() {
    return serverPort;
  }
}
