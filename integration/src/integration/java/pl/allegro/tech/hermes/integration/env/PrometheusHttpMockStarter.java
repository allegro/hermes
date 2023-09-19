package pl.allegro.tech.hermes.integration.env;

import pl.allegro.tech.hermes.test.helper.environment.WireMockStarter;

public class PrometheusHttpMockStarter extends WireMockStarter implements EnvironmentAware {

    public PrometheusHttpMockStarter() {
        super(PROMETHEUS_HTTP_SERVER_PORT);
    }
}
