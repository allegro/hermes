package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.pubsub.v1.PubsubMessage;
import pl.allegro.tech.hermes.consumers.consumer.Message;

interface GooglePubSubMessageTransformer {

  PubsubMessage fromHermesMessage(Message message);
}
