package pl.allegro.tech.hermes.consumers.consumer.sender.jms;

import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;

import java.net.URI;
import javax.jms.ConnectionFactory;

public interface JmsMessageSenderProvider extends ProtocolMessageSenderProvider {

    ConnectionFactory createConnectionFactory(URI serverUri);

}
