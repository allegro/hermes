package pl.allegro.tech.hermes.message.tracker.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.util.Arrays;

public class ElasticsearchClientFactory {

    private final TransportClient client;

    public ElasticsearchClientFactory(int port, String... hosts) {
        client = new TransportClient();
        Arrays.stream(hosts).forEach(host ->
                client.addTransportAddress(new InetSocketTransportAddress(host, port)));
    }

    public Client client() {
        return client;
    }

    public void close() {
        if (client != null) client.close();
    }
}
