package pl.allegro.tech.hermes.consumers.consumer.batch;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import javax.inject.Inject;
import java.time.Clock;

public class ByteBufferMessageBatchFactoryProvider implements Factory<MessageBatchFactory> {

    private final HermesMetrics hermesMetrics;
    private final Clock clock;
    private final int poolableSize;
    private final int maxPoolSize;

    @Inject
    public ByteBufferMessageBatchFactoryProvider(HermesMetrics hermesMetrics, Clock clock, ConfigFactory configFactory) {
        this.hermesMetrics = hermesMetrics;
        this.clock = clock;
        this.poolableSize = configFactory.getIntProperty(Configs.CONSUMER_BATCH_POOLABLE_SIZE);
        this.maxPoolSize = configFactory.getIntProperty(Configs.CONSUMER_BATCH_MAX_POOL_SIZE);
    }

    @Override
    public ByteBufferMessageBatchFactory provide() {
        return new ByteBufferMessageBatchFactory(poolableSize, maxPoolSize, clock, hermesMetrics);
    }

    @Override
    public void dispose(MessageBatchFactory instance) {

    }
}
