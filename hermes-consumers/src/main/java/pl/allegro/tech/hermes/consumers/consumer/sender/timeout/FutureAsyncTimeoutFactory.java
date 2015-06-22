package pl.allegro.tech.hermes.consumers.consumer.sender.timeout;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_MONITORING;
import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_SIZE;

public class FutureAsyncTimeoutFactory implements Factory<FutureAsyncTimeout<MessageSendingResult>> {

    private final ScheduledExecutorService timeoutExecutorService;

    @Inject
    public FutureAsyncTimeoutFactory(ConfigFactory configFactory, InstrumentedExecutorServiceFactory executorFactory) {
        this.timeoutExecutorService = executorFactory.getScheduledExecutorService("async-timeout", configFactory.getIntProperty(CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_SIZE),
                configFactory.getBooleanProperty(CONSUMER_SENDER_ASYNC_TIMEOUT_THREAD_POOL_MONITORING));
    }

    @Override
    public FutureAsyncTimeout<MessageSendingResult> provide() {
        return new FutureAsyncTimeout<>(MessageSendingResult::loggedFailResult, timeoutExecutorService);
    }

    @Override
    public void dispose(FutureAsyncTimeout instance) {
        instance.shutdown();
    }

}
