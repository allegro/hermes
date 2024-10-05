package pl.allegro.tech.hermes.common.broker;

public class BrokerDetails {

  private String host;
  private int port;

  public BrokerDetails(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }
}
