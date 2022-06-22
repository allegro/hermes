package pl.allegro.tech.hermes.common.di.factories;

public class GraphiteParameters {

    private final String prefix;

    private final String host;

    private final int port;

    public String getPrefix() {
        return prefix;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public GraphiteParameters(String prefix, String host, int port) {
        this.prefix = prefix;
        this.host = host;
        this.port = port;
    }
}
