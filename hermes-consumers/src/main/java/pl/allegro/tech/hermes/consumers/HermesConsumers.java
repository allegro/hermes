package pl.allegro.tech.hermes.consumers;

import com.google.common.collect.Lists;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.jvnet.hk2.component.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.di.CommonBinder;
import pl.allegro.tech.hermes.common.hook.Hook;
import pl.allegro.tech.hermes.common.hook.HooksHandler;
import pl.allegro.tech.hermes.consumers.consumer.health.HealthCheckServer;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderProviders;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.di.ConsumersBinder;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.message.tracker.consumers.LogRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class HermesConsumers {

    private static final Logger logger = LoggerFactory.getLogger(HermesConsumers.class);

    private final HooksHandler hooksHandler;
    private final ConsumersSupervisor consumersSupervisor;
    private final HealthCheckServer healthCheckServer;
    private MultiMap<String, Supplier<ProtocolMessageSenderProvider>> messageSenderProvidersSuppliers;
    private final MessageSenderProviders messageSendersProviders;
    private final ServiceLocator serviceLocator;

    public static void main(String... args) {
        consumers().build().start();
    }

    private HermesConsumers(HooksHandler hooksHandler, List<Binder> binders, MultiMap<String, Supplier<ProtocolMessageSenderProvider>> messageSenderProvidersSuppliers) {

        this.hooksHandler = hooksHandler;
        this.messageSenderProvidersSuppliers = messageSenderProvidersSuppliers;
        this.serviceLocator = createDIContainer(binders);

        consumersSupervisor = serviceLocator.getService(ConsumersSupervisor.class);
        healthCheckServer = serviceLocator.getService(HealthCheckServer.class);
        messageSendersProviders = serviceLocator.getService(MessageSenderProviders.class);

        hooksHandler.addShutdownHook(() -> {
            try {
                healthCheckServer.stop();
                consumersSupervisor.shutdown();
                serviceLocator.shutdown();
            } catch (InterruptedException e) {
                logger.error("Exception while shutdown Hermes Consumers", e);
            }
        });
    }

    public void start() {
        try {

            messageSenderProvidersSuppliers.entrySet().stream().forEach(entry -> {
                entry.getValue().stream().forEach( supplier -> {
                    messageSendersProviders.put(entry.getKey(), supplier.get());
                });
            });

            consumersSupervisor.start();
            healthCheckServer.start();
            hooksHandler.startup();
        } catch (Exception e) {
            logger.error("Exception while starting Hermes Consumers", e);
        }
    }

    public void stop() {
        hooksHandler.shutdown();
    }

    private ServiceLocator createDIContainer(List<Binder> binders) {
        String uniqueName = "HermesConsumersLocator" + UUID.randomUUID();
        return ServiceLocatorUtilities.bind(uniqueName, binders.toArray(new Binder[binders.size()]));
    }

    public <T> T getService(Class<T> clazz) {
        return serviceLocator.getService(clazz);
    }

    public <T> T getService(Class<T> clazz, String name) {
        return serviceLocator.getService(clazz, name);
    }

    public static Builder consumers() {
        return new Builder();
    }

    public static final class Builder {
        private final HooksHandler hooksHandler = new HooksHandler();
        private final MessageSenderProviders messageSendersProviders = new MessageSenderProviders();
        private final MultiMap<String, Supplier<ProtocolMessageSenderProvider>> messageSenderProvidersSuppliers = new MultiMap<>();
        private final List<LogRepository> logRepositories = new ArrayList<>();

        private final List<Binder> binders = Lists.newArrayList(
                new CommonBinder(),
                new ConsumersBinder(),
                new ProtocolMessageSenderProvidersBinder());

        public Builder withShutdownHook(Hook hook) {
            hooksHandler.addShutdownHook(hook);
            return this;
        }

        public Builder withStartupHook(Hook hook) {
            hooksHandler.addStartupHook(hook);
            return this;
        }

        public HermesConsumers.Builder withMessageSenderProvider(String protocol, Supplier<ProtocolMessageSenderProvider> messageSenderProviderSupplier) {
            this.messageSenderProvidersSuppliers.add(protocol, messageSenderProviderSupplier);
            return this;
        }

        public HermesConsumers.Builder withLogRepository(LogRepository logRepository) {
            logRepositories.add(logRepository);
            return this;
        }

        public <T> Builder withBinding(T instance, Class<T> clazz) {
            return withBinding(instance, clazz, clazz.getName());
        }

        public <T> Builder withBinding(T instance, Class<T> clazz, String name) {
            final int rankHigherThanDefault = 10;
            binders.add(new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(instance).to(clazz).named(name).ranked(rankHigherThanDefault);
                }
            });
            return this;
        }

        public HermesConsumers build() {
            binders.add(new AbstractBinder() {
                @Override
                protected void configure() {

                }
            });

            return new HermesConsumers(hooksHandler, binders, messageSenderProvidersSuppliers);
        }

        private final class ProtocolMessageSenderProvidersBinder extends AbstractBinder {
            @Override
            protected void configure() {
                bind(messageSendersProviders).to(MessageSenderProviders.class);
            }
        }
    }

}
