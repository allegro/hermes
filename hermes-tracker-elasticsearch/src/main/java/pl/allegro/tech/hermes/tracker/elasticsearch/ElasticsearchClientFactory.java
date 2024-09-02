package pl.allegro.tech.hermes.tracker.elasticsearch;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

public class ElasticsearchClientFactory {

  private final TransportClient client;

  public ElasticsearchClientFactory(int port, String clusterName, String... hosts) {
    client =
        new PreBuiltTransportClient(Settings.builder().put("cluster.name", clusterName).build());

    for (String host : hosts) {
      try {
        client.addTransportAddress(new TransportAddress(InetAddress.getByName(host), port));
      } catch (UnknownHostException e) {
        throw new RuntimeException("Unknown host", e);
      }
    }
  }

  public Client client() {
    return client;
  }

  public void close() {
    if (client != null) {
      client.close();
    }
  }
}
