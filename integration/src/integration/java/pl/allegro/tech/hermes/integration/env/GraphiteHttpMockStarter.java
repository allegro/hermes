package pl.allegro.tech.hermes.integration.env;

import pl.allegro.tech.hermes.test.helper.environment.WireMockStarter;

import static pl.allegro.tech.hermes.integration.env.EnvironmentAware.GRAPHITE_HTTP_SERVER_PORT;

public class GraphiteHttpMockStarter extends WireMockStarter {

    public GraphiteHttpMockStarter() {
        super(GRAPHITE_HTTP_SERVER_PORT);
    }
}
