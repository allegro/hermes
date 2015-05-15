package pl.allegro.tech.hermes.integration.env;

import pl.allegro.tech.hermes.test.helper.environment.WireMockStarter;

public class OAuthServerMockStarter extends WireMockStarter implements EnvironmentAware {

    public OAuthServerMockStarter() {
        super(OAUTH_SERVER_PORT);
    }
}
