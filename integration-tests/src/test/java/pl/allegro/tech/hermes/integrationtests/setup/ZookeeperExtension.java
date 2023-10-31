package pl.allegro.tech.hermes.integrationtests.setup;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;

public class ZookeeperExtension implements BeforeAllCallback {
    public final ZookeeperContainer hermesZookeeperOne = new ZookeeperContainer("ZookeeperContainerOne");

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        hermesZookeeperOne.start();
    }
}
