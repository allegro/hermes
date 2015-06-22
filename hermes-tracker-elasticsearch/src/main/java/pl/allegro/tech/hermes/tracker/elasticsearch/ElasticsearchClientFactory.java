package pl.allegro.tech.hermes.tracker.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.util.Arrays;

public class ElasticsearchClientFactory {

    private final TransportClient client;

    public ElasticsearchClientFactory(int port, String clusterName, String... hosts) {
        Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).build();
        client = new TransportClient(settings);
        Arrays.stream(hosts).forEach(host -> client.addTransportAddress(new InetSocketTransportAddress(host, port)));
    }

    public Client client() {
        return client;
    }

    public void close() {
        if (client != null) client.close();
    }
}
