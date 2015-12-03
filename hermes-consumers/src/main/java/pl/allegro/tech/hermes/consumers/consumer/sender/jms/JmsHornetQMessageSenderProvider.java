package pl.allegro.tech.hermes.consumers.consumer.sender.jms;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;
import pl.allegro.tech.hermes.consumers.uri.UriUtils;

import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import java.net.URI;
import java.util.HashMap;

public class JmsHornetQMessageSenderProvider extends AbstractJmsMessageSenderProvider {

    @Inject
    public JmsHornetQMessageSenderProvider(ConfigFactory configFactory, MetadataAppender<Message> metadataAppender) {
        super(configFactory, metadataAppender);
    }

    @Override
    public ConnectionFactory createConnectionFactory(URI serverUri) {
        HashMap<String, Object> props = new HashMap<>();
        props.put("host", UriUtils.extractHostFromUri(serverUri));
        Integer port = UriUtils.extractPortFromUri(serverUri);
        if (port != null) {
            props.put("port", port);
        }
        TransportConfiguration transportConfiguration = new TransportConfiguration(NettyConnectorFactory.class.getName(), props);
        return HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);
    }

}
