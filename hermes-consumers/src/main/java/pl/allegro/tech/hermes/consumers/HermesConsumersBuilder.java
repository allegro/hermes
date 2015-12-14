package pl.allegro.tech.hermes.consumers;

import com.google.common.collect.Lists;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.jvnet.hk2.component.MultiMap;
import pl.allegro.tech.hermes.common.di.CommonBinder;
import pl.allegro.tech.hermes.common.hook.Hook;
import pl.allegro.tech.hermes.common.hook.HooksHandler;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderProviders;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.di.ConsumersBinder;
import pl.allegro.tech.hermes.consumers.di.TrackersBinder;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class HermesConsumersBuilder {

    private final HooksHandler hooksHandler = new HooksHandler();
    private final MessageSenderProviders messageSendersProviders = new MessageSenderProviders();
    private final MultiMap<String, Supplier<ProtocolMessageSenderProvider>> messageSenderProvidersSuppliers = new MultiMap<>();
    private final List<Function<ServiceLocator, LogRepository>>logRepositories = new ArrayList<>();

    private Optional<Function<ServiceLocator, KafkaNamesMapper>> kafkaNamesMapper = Optional.empty();

    private final List<Binder> binders = Lists.newArrayList(
            new CommonBinder(),
            new ConsumersBinder(),
            new ProtocolMessageSenderProvidersBinder());

    public HermesConsumersBuilder withShutdownHook(Hook hook) {
        hooksHandler.addShutdownHook(hook);
        return this;
    }

    public HermesConsumersBuilder withStartupHook(Hook hook) {
        hooksHandler.addStartupHook(hook);
        return this;
    }

    public HermesConsumersBuilder withMessageSenderProvider(String protocol, Supplier<ProtocolMessageSenderProvider> messageSenderProviderSupplier) {
        this.messageSenderProvidersSuppliers.add(protocol, messageSenderProviderSupplier);
        return this;
    }

    public HermesConsumersBuilder withLogRepository(Function<ServiceLocator, LogRepository> logRepository) {
        logRepositories.add(logRepository);
        return this;
    }

    public HermesConsumersBuilder withKafkaTopicsNamesMapper(Function<ServiceLocator, KafkaNamesMapper> kafkaNamesMapper) {
        this.kafkaNamesMapper = Optional.of(kafkaNamesMapper);
        return this;
    }

    public <T> HermesConsumersBuilder withBinding(T instance, Class<T> clazz) {
        return withBinding(instance, clazz, clazz.getName());
    }

    public <T> HermesConsumersBuilder withBinding(T instance, Class<T> clazz, String name) {
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
        binders.add(new TrackersBinder(new ArrayList<>()));
        return new HermesConsumers(hooksHandler, binders, messageSenderProvidersSuppliers, logRepositories, kafkaNamesMapper);
    }

    private final class ProtocolMessageSenderProvidersBinder extends AbstractBinder {
        @Override
        protected void configure() {
            bind(messageSendersProviders).to(MessageSenderProviders.class);
        }
    }
}
