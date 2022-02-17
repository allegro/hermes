package pl.allegro.tech.hermes.integration.env;

import pl.allegro.tech.hermes.test.helper.environment.WireMockStarter;

public class AuditEventMockStarter extends WireMockStarter implements EnvironmentAware {

    public AuditEventMockStarter() {
        super(AUDIT_EVENT_PORT);
    }
}
