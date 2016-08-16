package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.Metadata;
import org.apache.kafka.common.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

import static java.util.Collections.singletonList;

public class ProducerBrokerNodeReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerBrokerNodeReader.class);

    public static List<Node> read(Producer producer) {
        if (producer instanceof KafkaProducer) {
            KafkaProducer kafkaProducer = (KafkaProducer) producer;
            try {
                Field field = KafkaProducer.class.getDeclaredField("metadata");
                field.setAccessible(true);
                Metadata metadata = (Metadata) field.get(kafkaProducer);
                return metadata.fetch().nodes();
            } catch (Exception e) {
                LOGGER.error("Could not read broker node list from producer.", e);
            }
        }
        return singletonList(new Node(0, "none", 0));
    }
}
