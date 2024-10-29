package pl.allegro.tech.hermes.frontend.producer.kafka;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.PublishingChaosPolicy;
import pl.allegro.tech.hermes.api.PublishingChaosPolicy.ChaosPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.readiness.AdminReadinessService;

public class MultiDatacenterMessageProducer implements BrokerMessageProducer {

  private static final Logger logger =
      LoggerFactory.getLogger(MultiDatacenterMessageProducer.class);

  private final KafkaMessageSenders kafkaMessageSenders;
  private final MessageToKafkaProducerRecordConverter messageConverter;
  private final Duration speculativeSendDelay;
  private final AdminReadinessService adminReadinessService;
  private final ScheduledExecutorService fallbackScheduler;

  public MultiDatacenterMessageProducer(
      KafkaMessageSenders kafkaMessageSenders,
      AdminReadinessService adminReadinessService,
      MessageToKafkaProducerRecordConverter messageConverter,
      Duration speculativeSendDelay,
      ScheduledExecutorService fallbackScheduler) {
    this.messageConverter = messageConverter;
    this.kafkaMessageSenders = kafkaMessageSenders;
    this.speculativeSendDelay = speculativeSendDelay;
    this.adminReadinessService = adminReadinessService;
    this.fallbackScheduler = fallbackScheduler;
  }

  @Override
  public void send(Message message, CachedTopic cachedTopic, PublishingCallback callback) {
    var producerRecord =
        messageConverter.convertToProducerRecord(
            message, cachedTopic.getKafkaTopics().getPrimary().name());

    KafkaMessageSender<byte[], byte[]> localSender =
        kafkaMessageSenders.get(cachedTopic.getTopic());
    Optional<KafkaMessageSender<byte[], byte[]>> remoteSender = getRemoteSender(cachedTopic);

    Map<String, ChaosExperiment> experiments =
        createChaosExperimentsPerDatacenter(cachedTopic.getTopic());

    if (remoteSender.isPresent()) {
      sendWithFallback(
          localSender,
          remoteSender.get(),
          producerRecord,
          cachedTopic,
          message,
          experiments,
          callback);
    } else {
      sendWithoutFallback(localSender, producerRecord, cachedTopic, message, callback);
    }
  }

  private static class SendWithFallbackExecutionContext {

    private final AtomicBoolean fallbackExecuted = new AtomicBoolean(false);
    private final AtomicBoolean sent = new AtomicBoolean(false);
    private final AtomicInteger tries;
    private final ConcurrentHashMap<String, Exception> errors;

    private SendWithFallbackExecutionContext() {
      this.tries = new AtomicInteger(2);
      this.errors = new ConcurrentHashMap<>(2);
    }

    public boolean tryTransitionToFallback() {
      return fallbackExecuted.compareAndSet(false, true) && !sent.get();
    }

    boolean tryTransitionToUnpublished(String datacenter, Exception exception) {
      errors.put(datacenter, exception);
      return tries.decrementAndGet() == 0;
    }

    public boolean tryTransitionToFirstSent() {
      return sent.compareAndSet(false, true);
    }
  }

  /*
  We first try to send message to local DC. If the local send fails we perform 'immediate' fallback to remote DC.

  Additionally, we schedule a 'speculative' fallback to remote DC to execute after 'speculativeSendDelay' elapses.
  Speculative fallback decreases publication latency but may result in messages being duplicated across DCs.

  If local DC send succeeds or fails before 'speculativeSendDelay' elapses we try to cancel the 'speculative' fallback if
  it has not been executed yet. We guarantee that only one fallback executes - either 'immediate' or 'speculative'.
   */
  private void sendWithFallback(
      KafkaMessageSender<byte[], byte[]> localSender,
      KafkaMessageSender<byte[], byte[]> remoteSender,
      ProducerRecord<byte[], byte[]> producerRecord,
      CachedTopic cachedTopic,
      Message message,
      Map<String, ChaosExperiment> experiments,
      PublishingCallback publishingCallback) {

    SendWithFallbackExecutionContext context = new SendWithFallbackExecutionContext();

    FallbackRunnable fallback =
        new FallbackRunnable(
            remoteSender,
            producerRecord,
            cachedTopic,
            message,
            experiments.getOrDefault(remoteSender.getDatacenter(), ChaosExperiment.DISABLED),
            publishingCallback,
            context);

    Future<?> speculativeFallback;
    try {
      speculativeFallback =
          fallbackScheduler.schedule(
              fallback, speculativeSendDelay.toMillis(), TimeUnit.MILLISECONDS);
    } catch (RejectedExecutionException rejectedExecutionException) {
      logger.warn(
          "Failed to run schedule fallback for message: {}, topic: {}",
          message,
          cachedTopic.getQualifiedName(),
          rejectedExecutionException);
      speculativeFallback = CompletableFuture.completedFuture(null);
    }

    localSender.send(
        producerRecord,
        cachedTopic,
        message,
        new FallbackAwareLocalSendCallback(
            message,
            cachedTopic,
            localSender.getDatacenter(),
            context,
            publishingCallback,
            fallback,
            speculativeFallback),
        experiments.getOrDefault(localSender.getDatacenter(), ChaosExperiment.DISABLED));
  }

  private void sendWithoutFallback(
      KafkaMessageSender<byte[], byte[]> sender,
      ProducerRecord<byte[], byte[]> producerRecord,
      CachedTopic cachedTopic,
      Message message,
      PublishingCallback callback) {
    sender.send(
        producerRecord,
        cachedTopic,
        message,
        new LocalSendCallback(message, cachedTopic, sender.getDatacenter(), callback));
  }

  private Map<String, ChaosExperiment> createChaosExperimentsPerDatacenter(Topic topic) {
    PublishingChaosPolicy chaos = topic.getChaos();
    return switch (chaos.mode()) {
      case DISABLED -> Map.of();
      case GLOBAL -> {
        Map<String, ChaosExperiment> experiments = new HashMap<>();
        ChaosPolicy policy = chaos.globalPolicy();
        boolean enabled = computeIfShouldBeEnabled(policy);
        for (String datacenter : kafkaMessageSenders.getDatacenters()) {
          experiments.put(datacenter, createChaosExperimentForDatacenter(policy, enabled));
        }
        yield experiments;
      }
      case DATACENTER -> {
        Map<String, ChaosExperiment> experiments = new HashMap<>();
        Map<String, ChaosPolicy> policies = chaos.datacenterPolicies();
        for (String datacenter : kafkaMessageSenders.getDatacenters()) {
          ChaosPolicy policy = policies.get(datacenter);
          boolean enabled = computeIfShouldBeEnabled(policy);
          experiments.put(datacenter, createChaosExperimentForDatacenter(policy, enabled));
        }
        yield experiments;
      }
    };
  }

  private boolean computeIfShouldBeEnabled(ChaosPolicy policy) {
    if (policy == null) {
      return false;
    }
    return ThreadLocalRandom.current().nextInt(100) < policy.probability();
  }

  private ChaosExperiment createChaosExperimentForDatacenter(ChaosPolicy policy, boolean enabled) {
    if (!enabled) {
      return ChaosExperiment.DISABLED;
    }
    long delayMillisFrom = policy.delayFrom();
    long delayMillisTo = policy.delayTo();
    long delayMillis =
        ThreadLocalRandom.current().nextLong(delayMillisTo - delayMillisFrom) + delayMillisFrom;
    return new ChaosExperiment(true, policy.completeWithError(), delayMillis);
  }

  public record ChaosExperiment(boolean enabled, boolean completeWithError, long delayInMillis) {

    private static final ChaosExperiment DISABLED = new ChaosExperiment(false, false, 0);
  }

  private Optional<KafkaMessageSender<byte[], byte[]>> getRemoteSender(CachedTopic cachedTopic) {
    return kafkaMessageSenders.getRemote(cachedTopic.getTopic()).stream()
        .filter(producer -> adminReadinessService.isDatacenterReady(producer.getDatacenter()))
        .findFirst();
  }

  private record RemoteSendCallback(
      Message message,
      CachedTopic cachedTopic,
      String datacenter,
      PublishingCallback callback,
      SendWithFallbackExecutionContext state)
      implements Callback {

    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
      if (exception == null) {
        callback.onEachPublished(message, cachedTopic.getTopic(), datacenter);
        if (state.tryTransitionToFirstSent()) {
          callback.onPublished(message, cachedTopic.getTopic());
        } else {
          cachedTopic.markMessageDuplicated();
        }
      } else {
        if (state.tryTransitionToUnpublished(datacenter, exception)) {
          callback.onUnpublished(
              message, cachedTopic.getTopic(), new MultiDCPublishException(state.errors));
        }
      }
    }
  }

  private class FallbackAwareLocalSendCallback implements Callback {

    private final Message message;
    private final CachedTopic cachedTopic;
    private final String datacenter;
    private final PublishingCallback callback;
    private final FallbackRunnable fallback;
    private final Future<?> speculativeFallback;
    private final SendWithFallbackExecutionContext state;

    private FallbackAwareLocalSendCallback(
        Message message,
        CachedTopic cachedTopic,
        String datacenter,
        SendWithFallbackExecutionContext state,
        PublishingCallback callback,
        FallbackRunnable fallback,
        Future<?> speculativeFallback) {
      this.message = message;
      this.cachedTopic = cachedTopic;
      this.datacenter = datacenter;
      this.callback = callback;
      this.fallback = fallback;
      this.speculativeFallback = speculativeFallback;
      this.state = state;
    }

    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
      if (exception == null) {
        cancelSpeculativeFallback();
        callback.onEachPublished(message, cachedTopic.getTopic(), datacenter);
        if (state.tryTransitionToFirstSent()) {
          callback.onPublished(message, cachedTopic.getTopic());
        } else {
          cachedTopic.markMessageDuplicated();
        }
      } else {
        if (state.tryTransitionToUnpublished(datacenter, exception)) {
          callback.onUnpublished(
              message, cachedTopic.getTopic(), new MultiDCPublishException(state.errors));
        } else {
          fallback();
        }
      }
    }

    private void fallback() {
      try {
        speculativeFallback.cancel(false);
        fallbackScheduler.execute(fallback);
      } catch (RejectedExecutionException rejectedExecutionException) {
        logger.warn(
            "Failed to run immediate fallback for message: {}, topic: {}",
            message,
            cachedTopic.getQualifiedName(),
            rejectedExecutionException);
      }
    }

    private void cancelSpeculativeFallback() {
      speculativeFallback.cancel(false);
    }
  }

  private record LocalSendCallback(
      Message message, CachedTopic cachedTopic, String datacenter, PublishingCallback callback)
      implements Callback {

    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
      if (exception != null) {
        callback.onUnpublished(message, cachedTopic.getTopic(), exception);
      } else {
        callback.onEachPublished(message, cachedTopic.getTopic(), datacenter);
        callback.onPublished(message, cachedTopic.getTopic());
      }
    }
  }

  private class FallbackRunnable implements Runnable {
    private final KafkaMessageSender<byte[], byte[]> remoteSender;
    private final ProducerRecord<byte[], byte[]> producerRecord;
    private final CachedTopic cachedTopic;
    private final Message message;
    private final ChaosExperiment experiment;
    private final PublishingCallback callback;
    private final SendWithFallbackExecutionContext context;

    public FallbackRunnable(
        KafkaMessageSender<byte[], byte[]> remoteSender,
        ProducerRecord<byte[], byte[]> producerRecord,
        CachedTopic cachedTopic,
        Message message,
        ChaosExperiment experiment,
        PublishingCallback callback,
        SendWithFallbackExecutionContext context) {
      this.remoteSender = remoteSender;
      this.producerRecord = producerRecord;
      this.cachedTopic = cachedTopic;
      this.message = message;
      this.experiment = experiment;
      this.callback = callback;
      this.context = context;
    }

    public void run() {
      if (context.tryTransitionToFallback()) {
        remoteSender.send(
            producerRecord,
            cachedTopic,
            message,
            new RemoteSendCallback(
                message, cachedTopic, remoteSender.getDatacenter(), callback, context),
            experiment);
      }
    }
  }

  public static class MultiDCPublishException extends RuntimeException {

    public MultiDCPublishException(Map<String, Exception> exceptionsPerDC) {
      super(errorMessage(exceptionsPerDC));
    }

    private static String errorMessage(Map<String, Exception> exceptionsPerDC) {
      StringBuilder builder = new StringBuilder();
      exceptionsPerDC.forEach(
          (dc, exception) ->
              builder.append(String.format("[%s]: %s, ", dc, getRootCauseMessage(exception))));
      return builder.toString();
    }
  }

  @Override
  public boolean areAllTopicsAvailable() {
    return kafkaMessageSenders.areAllTopicsAvailable();
  }

  @Override
  public boolean isTopicAvailable(CachedTopic topic) {
    return kafkaMessageSenders.isTopicAvailable(topic);
  }
}
