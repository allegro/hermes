package pl.allegro.tech.hermes.mock;

import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;

public class HermesMockClassRule implements TestRule, ClassRule {

    private final HermesMock hermesMock;

    public HermesMockClassRule(int port) {
        this.hermesMock = new HermesMock.Builder().withPort(port).build();
    }

    public HermesMockClassRule(HermesMock hermesMock) {
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
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                createInitialContext();
                try {
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

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }
}
