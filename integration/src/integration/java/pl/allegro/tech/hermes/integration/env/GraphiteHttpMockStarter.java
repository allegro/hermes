package pl.allegro.tech.hermes.integration.env;

import pl.allegro.tech.hermes.test.helper.environment.WireMockStarter;

public class GraphiteHttpMockStarter extends WireMockStarter implements EnvironmentAware {

    public GraphiteHttpMockStarter() {
        super(GRAPHITE_HTTP_SERVER_PORT);
    }
}
