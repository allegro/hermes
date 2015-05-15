package pl.allegro.tech.hermes.consumers.consumer.offset;

import org.glassfish.hk2.api.Factory;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.KafkaAsyncOffsetMonitor;
import pl.allegro.tech.hermes.consumers.consumer.offset.kafka.KafkaLatestOffsetReader;

import javax.inject.Inject;

public class AsyncOffsetMonitorFactory implements Factory<AsyncOffsetMonitor> {

    private ConfigFactory configFactory;
    private KafkaLatestOffsetReader kafkaLatestOffsetReader;
    private HermesMetrics hermesMetrics;

    @Inject
    public AsyncOffsetMonitorFactory(ConfigFactory configFactory, KafkaLatestOffsetReader kafkaLatestOffsetReader,
                                     HermesMetrics hermesMetrics) {

        this.configFactory = configFactory;
        this.kafkaLatestOffsetReader = kafkaLatestOffsetReader;
        this.hermesMetrics = hermesMetrics;
    }

    @Override
    public AsyncOffsetMonitor provide() {
        if (configFactory.getBooleanProperty(Configs.CONSUMER_OFFSET_MONITOR_ENABLED)) {
            return new KafkaAsyncOffsetMonitor(kafkaLatestOffsetReader, hermesMetrics);
        }
        LoggerFactory.getLogger(AsyncOffsetMonitorFactory.class).info("Creating disabled offset monitor");
        return new DisabledAsyncOffsetMonitor();
    }

    @Override
    public void dispose(AsyncOffsetMonitor instance) {

    }
}
