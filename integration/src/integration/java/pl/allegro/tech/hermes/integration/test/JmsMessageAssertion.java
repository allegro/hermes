package pl.allegro.tech.hermes.integration.test;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Fail;

import javax.jms.JMSException;
import javax.jms.Message;

import static org.assertj.core.api.Assertions.assertThat;

public class JmsMessageAssertion extends AbstractAssert<JmsMessageAssertion, Message> {

    protected JmsMessageAssertion(Message actual) {
        super(actual, JmsMessageAssertion.class);
    }

    public JmsMessageAssertion assertStringProperty(String name, String value) {
        try {
            assertThat(actual.getStringProperty(name)).isEqualTo(value);
        } catch (JMSException e) {
            Fail.fail("Could not access property", e);
        }
        return this;
    }
}
