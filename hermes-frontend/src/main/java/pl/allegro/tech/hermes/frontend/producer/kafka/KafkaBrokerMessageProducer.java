package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.json.MessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.Message;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class KafkaBrokerMessageProducer implements BrokerMessageProducer {
    
    private final Producers producers;
    private final MessageContentWrapper contentWrapper;

    @Inject
    public KafkaBrokerMessageProducer(Producers producers, MessageContentWrapper contentWrapper, HermesMetrics metrics) {
        this.producers = producers;
        this.contentWrapper = contentWrapper;
        producers.registerGauges(metrics);
    }

    @Override
    public void send(Message message, Topic topic, final PublishingCallback... callbacks) {
        send(message, topic, Lists.newArrayList(callbacks));
    }

    private void send(Message message, Topic topic, final List<PublishingCallback> callbacks) {
        try {
            byte[] content = contentWrapper.wrapContent(message.getData(), getMetadata(message.getId(), message.getTimestamp()));
            ProducerRecord<byte[], byte[]> producerRecord = new ProducerRecord<>(topic.getQualifiedName(), content);
            producers.get(topic).send(producerRecord, new SendCallback(message, topic, callbacks));
        } catch (Exception e) {
            callbacks.forEach(c -> c.onUnpublished(e));
        }
    }

    private Map<String, Object> getMetadata(String messageId, long timestamp) {
        return ImmutableMap.of("id", messageId, "timestamp", timestamp);
    }

    private static class SendCallback implements org.apache.kafka.clients.producer.Callback {
        
        private final Message message;
        private final Topic topic;
        private final List<PublishingCallback> callbacks;

        public SendCallback(Message message, Topic topic, List<PublishingCallback> callbacks) {
            this.message = message;
            this.topic = topic;
            this.callbacks = callbacks;
        }

        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            if (e != null) {
                callbacks.forEach(c -> c.onUnpublished(e));
            } else {
                callbacks.forEach(c -> c.onPublished(message, topic));
            }
        }
    }
}
