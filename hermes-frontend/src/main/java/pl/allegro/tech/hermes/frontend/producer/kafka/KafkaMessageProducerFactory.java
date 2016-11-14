package pl.allegro.tech.hermes.frontend.producer.kafka;

import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.producer.ProducerConfig.*;
import static pl.allegro.tech.hermes.common.config.Configs.*;

public class KafkaMessageProducerFactory implements Factory<Producers> {
    private static final String ACK_ALL = "-1";
    private static final String ACK_LEADER = "1";

    @Inject
    private ConfigFactory configFactory;

    @Override
    public Producers provide() {
        Map<String, Object> common = new HashMap<>();
        common.put(BOOTSTRAP_SERVERS_CONFIG, getString(KAFKA_BROKER_LIST));
        common.put(MAX_BLOCK_MS_CONFIG, getInt(KAFKA_PRODUCER_MAX_BLOCK_MS));
        common.put(COMPRESSION_TYPE_CONFIG, getString(KAFKA_PRODUCER_COMPRESSION_CODEC));
        common.put(BUFFER_MEMORY_CONFIG, configFactory.getLongProperty(KAFKA_PRODUCER_BUFFER_MEMORY));
        common.put(REQUEST_TIMEOUT_MS_CONFIG, getInt(KAFKA_PRODUCER_REQUEST_TIMEOUT_MS));
        common.put(BATCH_SIZE_CONFIG, getInt(KAFKA_PRODUCER_BATCH_SIZE));
        common.put(SEND_BUFFER_CONFIG, getInt(KAFKA_PRODUCER_TCP_SEND_BUFFER));
        common.put(RETRIES_CONFIG, getInt(KAFKA_PRODUCER_RETRIES));
        common.put(RETRY_BACKOFF_MS_CONFIG, getInt(KAFKA_PRODUCER_RETRY_BACKOFF_MS));
        common.put(METADATA_MAX_AGE_CONFIG, getInt(KAFKA_PRODUCER_METADATA_MAX_AGE));
        common.put(KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        common.put(VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        common.put(MAX_REQUEST_SIZE_CONFIG, getInt(KAFKA_PRODUCER_MAX_REQUEST_SIZE));
        common.put(LINGER_MS_CONFIG, getInt(KAFKA_PRODUCER_LINGER_MS));
        common.put(METRICS_SAMPLE_WINDOW_MS_CONFIG, getInt(KAFKA_PRODUCER_METRICS_SAMPLE_WINDOW_MS));
        common.put(MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, getInt(KAFKA_PRODUCER_MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION));

        Producer<byte[], byte[]> leaderConfirms = new KafkaProducer<>(copyWithEntryAdded(common, ACKS_CONFIG, ACK_LEADER));
        Producer<byte[], byte[]> everyoneConfirms = new KafkaProducer<>(copyWithEntryAdded(common, ACKS_CONFIG, ACK_ALL));
        return new Producers(leaderConfirms, everyoneConfirms, configFactory);
    }

    private ImmutableMap<String, Object> copyWithEntryAdded(Map<String, Object> common, String key, String value) {
        return ImmutableMap.<String, Object>builder().putAll(common).put(key, value).build();
    }

    private String getString(Configs key) {
        return configFactory.getStringProperty(key);
    }

    private Integer getInt(Configs key) {
        return configFactory.getIntProperty(key);
    }

    @Override
    public void dispose(Producers producer) {
        producer.close();
    }
}
