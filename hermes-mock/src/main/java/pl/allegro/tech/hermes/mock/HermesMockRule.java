package pl.allegro.tech.hermes.mock;

import org.apache.avro.Schema;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.util.function.Predicate;

public class HermesMockRule implements MethodRule, TestRule {

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

    public <T> void resetReceivedAvroRequests(String topicName, Schema schema, Class<T> clazz, Predicate<T> predicate) {
        hermesMock.resetReceivedAvroRequests(topicName, schema, clazz, predicate);
    }

    public <T> void resetReceivedJsonRequests(String topicName, Class<T> clazz, Predicate<T> predicate) {
        hermesMock.resetReceivedJsonRequests(topicName, clazz, predicate);
    }

    public void resetMappings() {
        hermesMock.resetMappings();
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return runHermesMock(base);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return runHermesMock(base);
    }

    private Statement runHermesMock(Statement base) {
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
