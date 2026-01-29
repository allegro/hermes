package pl.allegro.tech.hermes.management.domain.health;

import java.net.InetAddress;
import java.net.UnknownHostException;
public class NodeDataProvider {

  private final String serverPort;
  public NodeDataProvider(String serverPort) {
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
