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

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BATCH_SIZE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BUFFER_MEMORY_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.COMPRESSION_TYPE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.LINGER_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.MAX_BLOCK_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION;
import static org.apache.kafka.clients.producer.ProducerConfig.MAX_REQUEST_SIZE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.METADATA_MAX_AGE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.RETRY_BACKOFF_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.SEND_BUFFER_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_AUTHORIZATION_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_AUTHORIZATION_MECHANISM;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_AUTHORIZATION_PASSWORD;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_AUTHORIZATION_PROTOCOL;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_AUTHORIZATION_USERNAME;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_BROKER_LIST;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_PRODUCER_BATCH_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_PRODUCER_COMPRESSION_CODEC;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_PRODUCER_LINGER_MS;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_PRODUCER_MAX_BLOCK_MS;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_PRODUCER_MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_PRODUCER_MAX_REQUEST_SIZE;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_PRODUCER_METADATA_MAX_AGE;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_PRODUCER_METRICS_SAMPLE_WINDOW_MS;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_PRODUCER_REQUEST_TIMEOUT_MS;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_PRODUCER_RETRIES;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_PRODUCER_RETRY_BACKOFF_MS;
import static pl.allegro.tech.hermes.common.config.Configs.KAFKA_PRODUCER_TCP_SEND_BUFFER;
import static pl.allegro.tech.hermes.common.config.Configs.MESSAGES_LOCAL_BUFFERED_STORAGE_SIZE;

public class KafkaMessageProducerFactory implements Factory<Producers> {
    private static final String ACK_ALL = "-1";
    private static final String ACK_LEADER = "1";

    private ConfigFactory configFactory;

    @Inject
    public KafkaMessageProducerFactory(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }

    @Override
    public Producers provide() {
        Map<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, getString(KAFKA_BROKER_LIST));
        props.put(MAX_BLOCK_MS_CONFIG, getInt(KAFKA_PRODUCER_MAX_BLOCK_MS));
        props.put(COMPRESSION_TYPE_CONFIG, getString(KAFKA_PRODUCER_COMPRESSION_CODEC));
        props.put(BUFFER_MEMORY_CONFIG, configFactory.getLongProperty(MESSAGES_LOCAL_BUFFERED_STORAGE_SIZE));
        props.put(REQUEST_TIMEOUT_MS_CONFIG, getInt(KAFKA_PRODUCER_REQUEST_TIMEOUT_MS));
        props.put(BATCH_SIZE_CONFIG, getInt(KAFKA_PRODUCER_BATCH_SIZE));
        props.put(SEND_BUFFER_CONFIG, getInt(KAFKA_PRODUCER_TCP_SEND_BUFFER));
        props.put(RETRIES_CONFIG, getInt(KAFKA_PRODUCER_RETRIES));
        props.put(RETRY_BACKOFF_MS_CONFIG, getInt(KAFKA_PRODUCER_RETRY_BACKOFF_MS));
        props.put(METADATA_MAX_AGE_CONFIG, getInt(KAFKA_PRODUCER_METADATA_MAX_AGE));
        props.put(KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put(MAX_REQUEST_SIZE_CONFIG, getInt(KAFKA_PRODUCER_MAX_REQUEST_SIZE));
        props.put(LINGER_MS_CONFIG, getInt(KAFKA_PRODUCER_LINGER_MS));
        props.put(METRICS_SAMPLE_WINDOW_MS_CONFIG, getInt(KAFKA_PRODUCER_METRICS_SAMPLE_WINDOW_MS));
        props.put(MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, getInt(KAFKA_PRODUCER_MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION));

        if (getBoolean(KAFKA_AUTHORIZATION_ENABLED)) {
            props.put(SASL_MECHANISM, getString(KAFKA_AUTHORIZATION_MECHANISM));
            props.put(SECURITY_PROTOCOL_CONFIG, getString(KAFKA_AUTHORIZATION_PROTOCOL));
            props.put(SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.plain.PlainLoginModule required\n"
                            + "username=\"" + getString(KAFKA_AUTHORIZATION_USERNAME) + "\"\n"
                            + "password=\"" + getString(KAFKA_AUTHORIZATION_PASSWORD) + "\";"
            );
        }

        Producer<byte[], byte[]> leaderConfirms = new KafkaProducer<>(copyWithEntryAdded(props, ACKS_CONFIG, ACK_LEADER));
        Producer<byte[], byte[]> everyoneConfirms = new KafkaProducer<>(copyWithEntryAdded(props, ACKS_CONFIG, ACK_ALL));
        return new Producers(leaderConfirms, everyoneConfirms, configFactory);
    }

    private ImmutableMap<String, Object> copyWithEntryAdded(Map<String, Object> common, String key, String value) {
        return ImmutableMap.<String, Object>builder().putAll(common).put(key, value).build();
    }

    private Boolean getBoolean(Configs key) {
        return configFactory.getBooleanProperty(key);
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
