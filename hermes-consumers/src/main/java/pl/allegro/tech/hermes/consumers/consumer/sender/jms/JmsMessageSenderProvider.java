package pl.allegro.tech.hermes.consumers.consumer.sender.jms;

import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;

import javax.jms.ConnectionFactory;
import java.net.URI;

public interface JmsMessageSenderProvider extends ProtocolMessageSenderProvider {

   ConnectionFactory createConnectionFactory(URI serverUri);

}
