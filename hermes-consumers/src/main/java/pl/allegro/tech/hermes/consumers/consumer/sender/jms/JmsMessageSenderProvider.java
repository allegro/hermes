package pl.allegro.tech.hermes.consumers.consumer.sender.jms;

import java.net.URI;
import javax.jms.ConnectionFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;

public interface JmsMessageSenderProvider extends ProtocolMessageSenderProvider {

  ConnectionFactory createConnectionFactory(URI serverUri);
}
