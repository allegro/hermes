package pl.allegro.tech.hermes.mock;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class HermesMockRule extends HermesMock implements MethodRule {

    public HermesMockRule() {
        super();
    }

    public HermesMockRule(int port) {
        super(port);
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    start();
                    resetReceivedRequest();
                    base.evaluate();
                } finally {
                    stop();
                }
            }
        };
    }
}
