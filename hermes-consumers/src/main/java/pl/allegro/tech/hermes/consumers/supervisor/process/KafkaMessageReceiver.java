package pl.allegro.tech.hermes.consumers.supervisor.process;

import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class KafkaMessageReceiver {
    Optional<ConsumerRecord<String, String>> next() {
        return Optional.empty();
    }
}
