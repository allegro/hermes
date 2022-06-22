package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.di.factories.GraphiteParameters;

@ConfigurationProperties(prefix = "frontend.graphite")
public class GraphiteProperties {

    private String prefix = "stats.tech.hermes";

    private String host = "localhost";

    private int port = 2003;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    protected GraphiteParameters toGraphiteParameters() {
        return new GraphiteParameters(
                this.prefix,
                this.host,
                this.port
        );
    }
}
