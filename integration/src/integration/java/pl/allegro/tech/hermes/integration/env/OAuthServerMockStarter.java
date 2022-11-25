package pl.allegro.tech.hermes.integration.env;

import pl.allegro.tech.hermes.test.helper.environment.WireMockStarter;

import static pl.allegro.tech.hermes.integration.env.EnvironmentAware.OAUTH_SERVER_PORT;

public class OAuthServerMockStarter extends WireMockStarter {

    public OAuthServerMockStarter() {
        super(OAUTH_SERVER_PORT);
    }
}
