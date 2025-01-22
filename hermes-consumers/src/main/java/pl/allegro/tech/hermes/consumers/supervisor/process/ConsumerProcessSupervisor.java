package pl.allegro.tech.hermes.consumers.supervisor.process;

import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.START;
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.STOP;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.queue.MonitoredMpscQueue;
import pl.allegro.tech.hermes.consumers.queue.MpscQueue;
import pl.allegro.tech.hermes.consumers.queue.WaitFreeDrainMpscQueue;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersExecutorService;
import pl.allegro.tech.hermes.metrics.HermesCounter;

public class ConsumerProcessSupervisor implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ConsumerProcessSupervisor.class);

  private final MpscQueue<Signal> taskQueue;

  private final RunningConsumerProcesses runningConsumerProcesses;

  private final ConsumerProcessKiller processKiller;

  private final ConsumersExecutorService executor;

  private final Clock clock;

  private final MetricsFacade metrics;

  private final SignalsFilter signalsFilter;

  private final ConsumerProcessSupplier processFactory;

  private final Map<String, HermesCounter> processedSignalsCounters = new HashMap<>();

  private final Map<String, HermesCounter> droppedSignalsCounters = new HashMap<>();

  public ConsumerProcessSupervisor(
      ConsumersExecutorService executor,
      Clock clock,
      MetricsFacade metrics,
      ConsumerProcessSupplier processFactory,
      int signalQueueSize,
      Duration backgroundSupervisorKillAfter) {
    this.executor = executor;
    this.clock = clock;
    this.metrics = metrics;
    this.taskQueue =
        new MonitoredMpscQueue<>(
            new WaitFreeDrainMpscQueue<>(signalQueueSize), metrics, "signalQueue");
    this.signalsFilter = new SignalsFilter(taskQueue, clock);
    this.runningConsumerProcesses = new RunningConsumerProcesses(clock);
    this.processKiller = new ConsumerProcessKiller(backgroundSupervisorKillAfter.toMillis(), clock);
    this.processFactory = processFactory;

    metrics
        .consumer()
        .registerRunningConsumerProcessesGauge(
            runningConsumerProcesses, RunningConsumerProcesses::count);
    metrics
        .consumer()
        .registerDyingConsumerProcessesGauge(processKiller, ConsumerProcessKiller::countDying);
  }

  public ConsumerProcessSupervisor accept(Signal signal) {
    taskQueue.offer(signal);
    return this;
  }

  public Set<SubscriptionName> existingConsumers() {
    return runningConsumerProcesses.existingConsumers();
  }

  @Override
  public void run() {
    logger.debug("Starting process supervisor loop");
    final long currentTime = clock.millis();

    restartUnhealthy();
    processKiller.killAllDying();

    List<Signal> signalsToProcess = new ArrayList<>();
    taskQueue.drain(signalsToProcess::add);
    signalsFilter.filterSignals(signalsToProcess).forEach(this::tryToProcessSignal);

    logger.debug(
        "Process supervisor loop took {} ms to check all consumers", clock.millis() - currentTime);
  }

  public void shutdown() {
    runningConsumerProcesses.stream()
        .forEach(
            p ->
                p.getConsumerProcess()
                    .accept(Signal.of(STOP, p.getConsumerProcess().getSubscriptionName())));

    processKiller.killAllDying();
    executor.shutdown();
  }

  public List<RunningSubscriptionStatus> runningSubscriptionsStatus() {
    return runningConsumerProcesses.listRunningSubscriptions();
  }

  public Integer countRunningProcesses() {
    return runningConsumerProcesses.count();
  }

  private void restartUnhealthy() {
    runningConsumerProcesses.stream()
        .filter(process -> !process.getConsumerProcess().isHealthy())
        .toList()
        .forEach(
            process -> {
              logger.info(
                  "Lost contact with consumer {} (last seen {}ms ago). Attempting to kill this process and spawn new one.",
                  process.getConsumerProcess(),
                  process.getConsumerProcess().lastSeen());
              processKiller.kill(process);
              runningConsumerProcesses.remove(process);
              taskQueue.offer(
                  Signal.of(
                      START,
                      process.getSubscription().getQualifiedName(),
                      process.getSubscription()));
            });
  }

  private void tryToProcessSignal(Signal signal) {
    try {
      processSignal(signal);
    } catch (Exception exception) {
      logger.error("Supervisor failed to process signal {}", signal, exception);
    }
  }

  private void processSignal(Signal signal) {
    logger.debug("Processing signal: {}", signal);
    processedSignalsCounters
        .computeIfAbsent(
            signal.getType().name(), name -> metrics.consumer().processedSignalsCounter(name))
        .increment();

    switch (signal.getType()) {
      case START:
        start(signal);
        break;
      case UPDATE_SUBSCRIPTION:
        updateSubscription(signal);
        break;
      case UPDATE_TOPIC:
      case RETRANSMIT:
        forRunningConsumerProcess(
            signal, runningProcess -> runningProcess.getConsumerProcess().accept(signal));
        break;
      case STOP:
        stop(signal);
        break;
      default:
        logger.warn("Unknown signal {}", signal);
        break;
    }
  }

  private void updateSubscription(Signal signal) {
    if (runningConsumerProcesses.hasProcess(signal.getTarget())) {
      stopOrUpdateConsumer(signal);
    } else {
      drop(signal);
    }
  }

  private void stopOrUpdateConsumer(Signal signal) {
    Subscription signalSubscription = signal.getPayload();
    if (!deliveryTypesEqual(signalSubscription)) {
      logger.info(
          "Stopping subscription: {} because of delivery type update",
          signalSubscription.getQualifiedName());
      stop(Signal.of(STOP, signal.getTarget()));
    } else {
      forRunningConsumerProcess(
          signal, runningProcess -> runningProcess.getConsumerProcess().accept(signal));
    }
  }

  private boolean deliveryTypesEqual(Subscription signalSubscription) {
    return signalSubscription.getDeliveryType()
        == runningConsumerProcesses
            .getProcess(signalSubscription.getQualifiedName())
            .getSubscription()
            .getDeliveryType();
  }

  private void stop(Signal signal) {
    forRunningConsumerProcess(
        signal,
        runningProcess -> {
          processKiller.observe(runningProcess);
          runningConsumerProcesses.remove(runningProcess);
          runningProcess.getConsumerProcess().accept(signal);
        });
  }

  private void forRunningConsumerProcess(
      Signal signal, java.util.function.Consumer<RunningConsumerProcess> consumerProcessConsumer) {
    if (runningConsumerProcesses.hasProcess(signal.getTarget())) {
      consumerProcessConsumer.accept(runningConsumerProcesses.getProcess(signal.getTarget()));
    } else {
      drop(signal);
    }
  }

  private void drop(Signal signal) {
    droppedSignalsCounters
        .computeIfAbsent(
            signal.getType().name(), name -> metrics.consumer().droppedSignalsCounter(name))
        .increment();
    logger.warn("Dropping signal {} as running target consumer process does not exist.", signal);
  }

  private void start(Signal start) {
    Subscription subscription = getSubscriptionFromPayload(start);

    if (!hasProcess(start.getTarget())) {
      try {
        logger.info("Creating consumer for {}", subscription.getQualifiedName());
        ConsumerProcess process =
            processFactory.createProcess(subscription, start, processKiller::cleanup);
        logger.info(
            "Created consumer for {}. {}",
            subscription.getQualifiedName(),
            start.getLogWithIdAndType());

        logger.info(
            "Starting consumer process for subscription {}. {}",
            start.getTarget(),
            start.getLogWithIdAndType());
        Future<?> future = executor.execute(process);
        logger.info(
            "Consumer for {} was added for execution. {}",
            subscription.getQualifiedName(),
            start.getLogWithIdAndType());

        runningConsumerProcesses.add(process, future);
        logger.info(
            "Started consumer process for subscription {}. {}",
            start.getTarget(),
            start.getLogWithIdAndType());
      } catch (Exception ex) {
        logger.error(
            "Failed to create consumer for subscription {}", subscription.getQualifiedName(), ex);
      }
    } else if (processKiller.isDying(start.getTarget())) {
      logger.info(
          "Consumer process for {} is already dying, startup deferred.",
          subscription.getQualifiedName());
      accept(start);
    } else {
      logger.info(
          "Abort consumer process start: process for subscription {} is already running. {}",
          start.getTarget(),
          start.getLogWithIdAndType());
    }
  }

  private boolean hasProcess(SubscriptionName subscriptionName) {
    return runningConsumerProcesses.hasProcess(subscriptionName)
        || processKiller.isDying(subscriptionName);
  }

  private Subscription getSubscriptionFromPayload(Signal startSignal) {
    if (!(startSignal.getPayload() instanceof Subscription)) {
      throw new IllegalArgumentException("Signal's payload has to be Subscription type");
    }
    return startSignal.getPayload();
  }
}
