package pl.allegro.tech.hermes.integration.helper;

import static com.jayway.awaitility.Awaitility.await;

import com.jayway.awaitility.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

public class RemoteJmsEndpoint {

    private final List<String> receivedMessages = new ArrayList<>();

    private int expectedMessagesCount = 0;

    public RemoteJmsEndpoint(String topicName) {
        initializeContext(topicName);
    }

    private JMSContext initializeContext(String topicName) {
        TransportConfiguration transportConfiguration = new TransportConfiguration(NettyConnectorFactory.class.getName());
        ConnectionFactory connectionFactory = HornetQJMSClient.createConnectionFactoryWithHA(
                JMSFactoryType.TOPIC_CF,
                transportConfiguration);

        JMSContext jmsContext = connectionFactory.createContext();

        Topic topic = jmsContext.createTopic(topicName);
        jmsContext.createConsumer(topic).setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                receivedMessages.add(readMessage(message));
            }
        });

        return jmsContext;
    }

    private String readMessage(Message message) {
        try {
            BytesMessage byteMessage = (BytesMessage) message;
            byte[] buffer = new byte[(int) byteMessage.getBodyLength()];
            byteMessage.readBytes(buffer);
            return new String(buffer);
        } catch (JMSException jmsException) {
            throw new RuntimeException(jmsException);
        }
    }

    public void expectMessages(TestMessage... messages) {
        expectedMessagesCount += messages.length;
    }

    public void waitUntilReceived() {
        await().atMost(Duration.FIVE_SECONDS).until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return receivedMessages.size() == expectedMessagesCount;
            }
        });
    }
}
