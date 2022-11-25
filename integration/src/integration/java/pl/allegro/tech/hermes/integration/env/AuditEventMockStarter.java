package pl.allegro.tech.hermes.integration.env;

import pl.allegro.tech.hermes.test.helper.environment.WireMockStarter;

import static pl.allegro.tech.hermes.integration.env.EnvironmentAware.AUDIT_EVENT_PORT;

public class AuditEventMockStarter extends WireMockStarter {

    public AuditEventMockStarter() {
        super(AUDIT_EVENT_PORT);
    }
}
