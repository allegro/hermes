package pl.allegro.tech.hermes.consumers;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.glassfish.hk2.api.ServiceLocator;//TODO: remove
import org.glassfish.hk2.utilities.Binder;//TODO: remove
import org.glassfish.hk2.utilities.binding.AbstractBinder;//TODO: remove
import org.jvnet.hk2.component.MultiMap;//TODO: remove
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import pl.allegro.tech.hermes.common.di.CommonBinder;
import pl.allegro.tech.hermes.common.hook.Hook;
import pl.allegro.tech.hermes.common.hook.HooksHandler;
import pl.allegro.tech.hermes.common.hook.ServiceAwareHook;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.consumers.di.config.PrimaryBeanCustomizer;
import pl.allegro.tech.hermes.consumers.hooks.SpringHooksHandler;
import pl.allegro.tech.hermes.consumers.hooks.SpringServiceAwareHook;
import pl.allegro.tech.hermes.domain.filtering.MessageFilter;
import pl.allegro.tech.hermes.domain.filtering.MessageFilterSource;
import pl.allegro.tech.hermes.domain.filtering.MessageFilters;
import pl.allegro.tech.hermes.domain.filtering.SubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.domain.filtering.avro.AvroPathSubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.domain.filtering.header.HeaderSubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.domain.filtering.json.JsonPathSubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.MessageContentReaderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.di.ConsumersBinder;
import pl.allegro.tech.hermes.consumers.di.TrackersBinder;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public final class HermesConsumersBuilder {//TODO: use java config + qualifiers instead?

//    private final GenericApplicationContext applicationContext;

    private static final int RANK_HIGHER_THAN_DEFAULT = 10;

    private final HooksHandler hooksHandler = new HooksHandler();
    private final SpringHooksHandler springHooksHandler = new SpringHooksHandler();
    //TODO: remove MultiMap...
    private final MultiMap<String, Function<ServiceLocator, ProtocolMessageSenderProvider>> messageSenderProviders = new MultiMap<>();
    private final Map<String, LinkedList<Function<ApplicationContext, ProtocolMessageSenderProvider>>> springMessageSenderProviders = new HashMap<>();//TODO: use guava?
    private final List<Function<ServiceLocator, LogRepository>> logRepositories = new ArrayList<>();//TODO
    private final List<Function<ApplicationContext, LogRepository>> springLogRepositories = new ArrayList<>();//TODO
    private final List<SubscriptionMessageFilterCompiler> filters = new ArrayList<>();
    private final List<MessageFilter> globalFilters = new ArrayList<>();
    private final List<ImmutablePair<Class<?>, Supplier<?>>> beanDefinitions = new ArrayList<>();

    private boolean flushLogsShutdownHookEnabled = true;

//    public HermesConsumersBuilder(GenericApplicationContext applicationContext) {
//        this.applicationContext = applicationContext;
//    }

    private final List<Binder> binders = Lists.newArrayList(
            new CommonBinder(),
            new ConsumersBinder());

    public HermesConsumersBuilder withStartupHook(ServiceAwareHook hook) {//TODO: replace ServiceAwareHook
        hooksHandler.addStartupHook(hook);
        return this;
    }

    public HermesConsumersBuilder withSpringStartupHook(SpringServiceAwareHook hook) {
        springHooksHandler.addStartupHook(hook);
        return this;
    }

    public HermesConsumersBuilder withStartupHook(Hook hook) {
        return withStartupHook(s -> hook.apply());
    }

    public HermesConsumersBuilder withSpringStartupHook(Hook hook) {
        return withSpringStartupHook(s -> hook.apply());
    }

    public HermesConsumersBuilder withShutdownHook(ServiceAwareHook hook) {
        hooksHandler.addShutdownHook(hook);
        return this;
    }

    public HermesConsumersBuilder withSpringShutdownHook(SpringServiceAwareHook hook) {
        springHooksHandler.addShutdownHook(hook);
        return this;
    }

    public HermesConsumersBuilder withShutdownHook(Hook hook) {
        return withShutdownHook(s -> hook.apply());
    }

    public HermesConsumersBuilder withSpringShutdownHook(Hook hook) {
        return withSpringShutdownHook(s -> hook.apply());
    }

    public HermesConsumersBuilder withDisabledGlobalShutdownHook() {
        hooksHandler.disableGlobalShutdownHook();
        return this;
    }

    public HermesConsumersBuilder withSpringDisabledGlobalShutdownHook() {
        springHooksHandler.disableGlobalShutdownHook();
        return this;
    }

    public HermesConsumersBuilder withDisabledFlushLogsShutdownHook() {
        flushLogsShutdownHookEnabled = false;
        return this;
    }

    public HermesConsumersBuilder withMessageSenderProvider(String protocol, Supplier<ProtocolMessageSenderProvider> messageSenderProviderSupplier) {
        this.messageSenderProviders.add(protocol, (s) -> messageSenderProviderSupplier.get());
        return this;
    }

//    public HermesConsumersBuilder withSpringMessageSenderProvider(String protocol, Supplier<ProtocolMessageSenderProvider> messageSenderProviderSupplier) {
//        this.springMessageSenderProviders.put(protocol, messageSenderProviderSupplier.get());
//        return this;
//    }

    public HermesConsumersBuilder withMessageSenderProvider(String protocol, Function<ServiceLocator, ProtocolMessageSenderProvider> messageSenderProviderSupplier) {
        this.messageSenderProviders.add(protocol, messageSenderProviderSupplier);
        return this;
    }

    public HermesConsumersBuilder withLogRepository(Function<ServiceLocator, LogRepository> logRepository) {
        logRepositories.add(logRepository);
        return this;
    }

    public HermesConsumersBuilder withSpringLogRepository(Function<ApplicationContext, LogRepository> logRepository) {
        springLogRepositories.add(logRepository);
        return this;
    }

    public HermesConsumersBuilder withSubscriptionMessageFilter(SubscriptionMessageFilterCompiler filter) {
        filters.add(filter);
        return this;
    }

    public HermesConsumersBuilder withGlobalMessageFilter(MessageFilter filter) {
        globalFilters.add(filter);
        return this;
    }

    public HermesConsumersBuilder withKafkaTopicsNamesMapper(KafkaNamesMapper kafkaNamesMapper) {
        return withBinding(kafkaNamesMapper, KafkaNamesMapper.class);
    }

    public HermesConsumersBuilder withSpringKafkaTopicsNamesMapper(Supplier<KafkaNamesMapper> kafkaNamesMapper) {
        return withSpringBinding(kafkaNamesMapper, KafkaNamesMapper.class);
    }

    public HermesConsumersBuilder withMessageContentReaderFactory(MessageContentReaderFactory messageContentReaderFactory) {
        return withBinding(messageContentReaderFactory, MessageContentReaderFactory.class);
    }

    public HermesConsumersBuilder withSpringMessageContentReaderFactory(MessageContentReaderFactory messageContentReaderFactory) {
        return withSpringBinding(() -> messageContentReaderFactory, MessageContentReaderFactory.class);
    }

    public <T> HermesConsumersBuilder withBinding(T instance, Class<T> clazz) { //TODO: check where it is used and why and what for
        return withBinding(instance, clazz, clazz.getName());
    }

    public <T> HermesConsumersBuilder withBinding(T instance, Class<T> clazz, String name) {
        binders.add(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(instance).to(clazz).named(name).ranked(RANK_HIGHER_THAN_DEFAULT);
            }
        });
        return this;
    }

    public <T> HermesConsumersBuilder withSpringBinding(Supplier<T> supplier, Class<T> clazz) {
//        BeanDefinitionCustomizer primaryBeanCustomizer = new PrimaryBeanCustomizer();
//        applicationContext.registerBean(clazz, supplier, primaryBeanCustomizer);
        beanDefinitions.add(new ImmutablePair<>(clazz, supplier));
        return this;
    }

    public HermesConsumers build() {
//        withBinding(buildFilters(), MessageFilterSource.class);
        withSpringBinding(this::buildFilters, MessageFilters.class);
//        withSpringBinding(() -> new Trackers(new ArrayList<>()), Trackers.class);

//        binders.add(new TrackersBinder(new ArrayList<>()));

//        messageSenderProviders.add(
//                "http", locator -> locator.getService(ProtocolMessageSenderProvider.class, "defaultHttpMessageSenderProvider")
//        );
//        messageSenderProviders.add(
//                "https", locator -> locator.getService(ProtocolMessageSenderProvider.class, "defaultHttpMessageSenderProvider")
//        );
//        messageSenderProviders.add(
//                "jms", locator -> locator.getService(ProtocolMessageSenderProvider.class, "defaultJmsMessageSenderProvider")
//        );

        LinkedList<Function<ApplicationContext, ProtocolMessageSenderProvider>> httpProviderList = new LinkedList<>();
        httpProviderList.add(applicationContext1 -> applicationContext1.getBean("defaultHttpMessageSenderProvider", ProtocolMessageSenderProvider.class));

        LinkedList<Function<ApplicationContext, ProtocolMessageSenderProvider>> httpsProviderList = new LinkedList<>();
        httpsProviderList.add(applicationContext1 -> applicationContext1.getBean("defaultHttpMessageSenderProvider", ProtocolMessageSenderProvider.class));

        LinkedList<Function<ApplicationContext, ProtocolMessageSenderProvider>> jmsProviderList = new LinkedList<>();
        jmsProviderList.add(applicationContext1 -> applicationContext1.getBean("defaultJmsMessageSenderProvider", ProtocolMessageSenderProvider.class));

        addSpringMessageSenderProvider("http", httpProviderList);
        addSpringMessageSenderProvider("https", httpsProviderList);
        addSpringMessageSenderProvider("jms", jmsProviderList);

        return new HermesConsumers(springHooksHandler, binders, beanDefinitions, springMessageSenderProviders, springLogRepositories, flushLogsShutdownHookEnabled);
    }

    private MessageFilters buildFilters() {
        List<SubscriptionMessageFilterCompiler> availableFilters = new ArrayList<>(filters);
        availableFilters.add(new JsonPathSubscriptionMessageFilterCompiler());
        availableFilters.add(new AvroPathSubscriptionMessageFilterCompiler());
        availableFilters.add(new HeaderSubscriptionMessageFilterCompiler());
        return new MessageFilters(globalFilters, availableFilters);
    }

    private void addSpringMessageSenderProvider(String key, LinkedList<Function<ApplicationContext, ProtocolMessageSenderProvider>> objects) {
        LinkedList<Function<ApplicationContext, ProtocolMessageSenderProvider>> currentList =
                springMessageSenderProviders.computeIfAbsent(key, k -> new LinkedList<>());
        currentList.addAll(objects);
    }
}
