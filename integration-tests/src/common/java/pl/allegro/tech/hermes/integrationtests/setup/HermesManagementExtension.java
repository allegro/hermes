package pl.allegro.tech.hermes.integrationtests.setup;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import pl.allegro.tech.hermes.test.helper.client.integration.HermesInitHelper;

public class HermesManagementExtension implements BeforeAllCallback, AfterAllCallback {

  private final HermesManagementTestApp management;
  private HermesInitHelper initHelper;
  private HermesFrontendTestApp frontendTestApp;

  public HermesManagementExtension(InfrastructureExtension infra) {
    management =
        new HermesManagementTestApp(infra.hermesZookeeper(), infra.kafka(), infra.schemaRegistry());
    frontendTestApp =
        new HermesFrontendTestApp(infra.hermesZookeeper(), infra.kafka(), infra.schemaRegistry());
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    management.start();
    initHelper = new HermesInitHelper(management.getPort(), frontendTestApp);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    management.stop();
  }

  public HermesInitHelper initHelper() {
    return initHelper;
  }
}
