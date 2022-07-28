package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.di.factories.GraphiteParameters;

@ConfigurationProperties(prefix = "consumer.graphite")
public class GraphiteProperties implements GraphiteParameters {

    private String prefix = "stats.tech.hermes";

    private String host = "localhost";

    private int port = 2003;

    @Override
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
