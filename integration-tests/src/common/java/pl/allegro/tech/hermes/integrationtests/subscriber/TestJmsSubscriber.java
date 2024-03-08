package pl.allegro.tech.hermes.integrationtests.subscriber;

import com.jayway.awaitility.Duration;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class TestJmsSubscriber {

    private static final int DEFAULT_WAIT_TIME_IN_SEC = 10;

    private final List<Message> receivedRequests = Collections.synchronizedList(new ArrayList<>());

    public TestJmsSubscriber(String topicName) {
        initializeContext(topicName);
    }

    private void initializeContext(String topicName) {
        TransportConfiguration transportConfiguration = new TransportConfiguration(NettyConnectorFactory.class.getName());
        ConnectionFactory connectionFactory = HornetQJMSClient.createConnectionFactoryWithHA(
                JMSFactoryType.TOPIC_CF,
                transportConfiguration);

        JMSContext jmsContext = connectionFactory.createContext();

        Topic topic = jmsContext.createTopic(topicName);
        jmsContext.createConsumer(topic).setMessageListener(this::onRequestReceived);

    }

    void onRequestReceived(Message request) {
        receivedRequests.add(request);
    }

    public void waitUntilReceived(String body) {
        awaitWithSyncRequests(() ->
                assertThat(
                        receivedRequests.stream()
                                .filter(r -> {
                                    try {
                                        String s = new String(r.getBody(byte[].class), StandardCharsets.UTF_8);
                                        return s.equals(body);
                                    } catch (JMSException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .findFirst()
                ).isNotEmpty());
    }

    public void waitUntilMessageWithHeaderReceived(String headerName, String headerValue) {
        awaitWithSyncRequests(() ->
                assertThat(
                        receivedRequests.stream()
                                .filter(r -> {
                                    try {
                                        return r.getStringProperty(headerName).equals(headerValue);
                                    } catch (JMSException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .findFirst())
                        .isNotEmpty());
    }

    private void awaitWithSyncRequests(Runnable runnable) {
        await().atMost(adjust(new Duration(DEFAULT_WAIT_TIME_IN_SEC, SECONDS))).until(() -> {
            synchronized (receivedRequests) {
                runnable.run();
            }
        });
    }
}