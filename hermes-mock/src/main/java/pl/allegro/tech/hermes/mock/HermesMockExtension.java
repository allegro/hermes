package pl.allegro.tech.hermes.mock;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class HermesMockExtension implements ParameterResolver, AfterAllCallback, AfterEachCallback {

    private final HermesMock hermesMock;

    public HermesMockExtension(int port) {
        this.hermesMock = new HermesMock.Builder().withPort(port).build();
        this.hermesMock.start();
    }

    public HermesMockExtension(HermesMock hermesMock) {
        this.hermesMock = hermesMock;
        this.hermesMock.start();
    }

    public HermesMockDefine define() {
        return hermesMock.define();
    }

    public HermesMockExpect expect() {
        return hermesMock.expect();
    }

    public HermesMockQuery query() {
        return hermesMock.query();
    }

    public void resetReceivedRequest() {
        hermesMock.resetReceivedRequest();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        hermesMock.stop();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        hermesMock.resetReceivedRequest();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().isAssignableFrom(HermesMock.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return hermesMock;
    }
}
