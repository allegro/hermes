package pl.allegro.tech.hermes.mock;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class HermesMockRule implements MethodRule {

    private final HermesMock hermesMock;

    public HermesMockRule(int port) {
        this.hermesMock = new HermesMock.Builder().withPort(port).build();
    }

    public HermesMockRule(HermesMock hermesMock) {
        this.hermesMock = hermesMock;
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
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                     createInitialContext();
                    base.evaluate();
                } finally {
                    destroyInitialContext();
                }
            }
        };
    }

    private void createInitialContext() {
        hermesMock.start();
        hermesMock.resetReceivedRequest();
    }

    private void destroyInitialContext() {
        hermesMock.stop();
    }
}
