package pl.allegro.tech.hermes.consumers.consumer.sender.jms;

import com.google.common.collect.ImmutableSet;
import java.net.URI;
import java.util.HashMap;
import java.util.Set;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;
import pl.allegro.tech.hermes.consumers.uri.UriUtils;

public class JmsHornetQMessageSenderProvider extends AbstractJmsMessageSenderProvider {
  private static final Set<String> SUPPORTED_PROTOCOLS = ImmutableSet.of("jms");

  public JmsHornetQMessageSenderProvider(MetadataAppender<Message> metadataAppender) {
    super(metadataAppender);
  }

  @Override
  public ConnectionFactory createConnectionFactory(URI serverUri) {
    HashMap<String, Object> props = new HashMap<>();
    props.put("host", UriUtils.extractHostFromUri(serverUri));
    Integer port = UriUtils.extractPortFromUri(serverUri);
    if (port != null) {
      props.put("port", port);
    }
    TransportConfiguration transportConfiguration =
        new TransportConfiguration(NettyConnectorFactory.class.getName(), props);
    return HornetQJMSClient.createConnectionFactoryWithoutHA(
        JMSFactoryType.CF, transportConfiguration);
  }

  @Override
  public Set<String> getSupportedProtocols() {
    return SUPPORTED_PROTOCOLS;
  }
}
