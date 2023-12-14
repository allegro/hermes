package pl.allegro.tech.hermes.integrationtests.setup;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import pl.allegro.tech.hermes.integrationtests.client.FrontendTestClient;

public class HermesFrontendExtension implements BeforeAllCallback, AfterAllCallback {

    private final HermesFrontendTestApp frontend;
    private FrontendTestClient client;

    public HermesFrontendExtension(InfrastructureExtension infra) {
        frontend = new HermesFrontendTestApp(infra.hermesZookeeper(), infra.kafka(), infra.schemaRegistry());
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        frontend.start();
        client = new FrontendTestClient(frontend.getPort());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        frontend.stop();
    }

    public FrontendTestClient api() {
        return client;
    }
}
