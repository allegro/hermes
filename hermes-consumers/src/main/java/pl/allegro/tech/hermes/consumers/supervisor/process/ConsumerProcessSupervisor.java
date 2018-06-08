package pl.allegro.tech.hermes.consumers.supervisor.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.queue.MonitoredMpscQueue;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerFactory;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersExecutorService;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.*;

public class ConsumerProcessSupervisor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerProcessSupervisor.class);

    private final MonitoredMpscQueue<Signal> taskQueue;

    private final ManagedConsumerProcesses consumerProcesses;

    private final Retransmitter retransmitter;

    private final ConsumersExecutorService executor;

    private final Clock clock;

    private final HermesMetrics metrics;

    private final SignalsFilter signalsFilter;

    private final long unhealthyAfter;

    private long killAfter;

    private ConsumerFactory consumerFactory;

    public ConsumerProcessSupervisor(ConsumersExecutorService executor,
                                     Retransmitter retransmitter,
                                     Clock clock,
                                     HermesMetrics metrics,
                                     ConfigFactory configs,
                                     ConsumerFactory consumerFactory) {
        this.executor = executor;
        this.retransmitter = retransmitter;
        this.clock = clock;
        this.metrics = metrics;
        this.unhealthyAfter = configs.getIntProperty(Configs.CONSUMER_BACKGROUND_SUPERVISOR_UNHEALTHY_AFTER);
        this.killAfter = configs.getIntProperty(Configs.CONSUMER_BACKGROUND_SUPERVISOR_KILL_AFTER);
        this.taskQueue = new MonitoredMpscQueue<>(metrics, "signalQueue",
                configs.getIntProperty(Configs.CONSUMER_SIGNAL_PROCESSING_QUEUE_SIZE));
        this.signalsFilter = new SignalsFilter(taskQueue, clock);
        this.consumerFactory = consumerFactory;
        this.consumerProcesses = new ManagedConsumerProcesses(clock);
    }

    public void accept(Signal signal) {
        taskQueue.offer(signal);
    }

    public Set<SubscriptionName> existingConsumers() {
        return consumerProcesses.runningSubscriptions();
    }

    @Override
    public void run() {
        logger.debug("Starting process supervisor loop");
        long currentTime = clock.millis();

        restartUnhealthy();
        superviseDyingProcesses();

        List<Signal> signalsToProcess = new ArrayList<>();
        taskQueue.drain(signalsToProcess::add);
        signalsFilter.filterSignals(signalsToProcess, consumerProcesses.runningSubscriptions(),
		        consumerProcesses.dyingSubscriptions()).forEach(this::tryToProcessSignal);

        logger.debug("Process supervisor loop took {} ms to check all consumers", clock.millis() - currentTime);
    }

    public void shutdown() {
        consumerProcesses.runningProcessesStream()
                .forEach(p -> p.accept(Signal.of(STOP, subscriptionName(p))));

        consumerProcesses.dyingProcessesStream()
                .forEach(p -> p.accept(Signal.of(FORCE_KILL_DYING, subscriptionName(p))));

        executor.shutdown();
    }

    public List<ManagedSubscriptionStatus> runningSubscriptionsStatus() {
        return consumerProcesses.listRunningSubscriptionsStatus();
    }

    public Integer countRunningProcesses() {
        return consumerProcesses.countRunningSubscriptions();
    }

    private void restartUnhealthy() {
        consumerProcesses.unhealthyProcessesStream()
                .forEach(consumerProcess -> {
                    Signal restartUnhealthy =
                            Signal.of(RESTART_UNHEALTHY, subscriptionName(consumerProcess));
                    logger.info("Lost contact with consumer {}, last seen {}ms ago {}. {}",
                            consumerProcess, consumerProcess.lastSeen(), restartUnhealthy.getLogWithIdAndType());
                    taskQueue.offer(restartUnhealthy);
                });
    }

    private void superviseDyingProcesses() {
        consumerProcesses.toForceKillProcessesStream()
                .forEach(consumerProcess -> {
                    Signal forceKillDying = Signal.of(FORCE_KILL_DYING, subscriptionName(consumerProcess));
                    logger.info("Consumer {} should be killed {}ms ago  but it is still dying.",
                            consumerProcess, clock.millis() - consumerProcesses.killTimeForDyingProcess(consumerProcess));
                    accept(forceKillDying);
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
        metrics.counter("supervisor.signal." + signal.getType().name()).inc();

        switch (signal.getType()) {
            case START:
                start(signal);
                break;
            case RETRANSMIT:
            case UPDATE_SUBSCRIPTION:
            case UPDATE_TOPIC:
            case RESTART:
            case COMMIT:
                forRunningConsumerProcess(signal, consumerProcess -> consumerProcess.accept(signal));
                break;
            case STOP:
                forRunningConsumerProcess(signal, consumerProcess -> {
                    // move to dying processes before shutdown callback will be called
                    consumerProcesses.moveToDyingProcesses(signal, forceKillTime());
                    consumerProcess.accept(signal);
                });
                break;
            case KILL:
                forRunningConsumerProcess(signal, consumerProcess -> {
                    killRunning(signal);
                });
                break;
            case RESTART_UNHEALTHY:
                forRunningConsumerProcess(signal, consumerProcess -> {
                    // we can't use KILL signal except calling killRunning(signal) because it has to be done
                    // before START signal will be pushed to the queue, so we call this immediately
                    killRunning(signal);
                    taskQueue.offer(signal.createChild(START, clock.millis(), consumerProcess.getSubscription()));
                });
                break;
            case FORCE_KILL_DYING:
                forDyingConsumerProcess(signal, consumerProcess -> {
                    forceKillDying(signal);
                });
                break;
            case CLEANUP:
                forDyingConsumerProcess(signal, consumerProcess -> cleanup(signal));
                break;
            default:
                logger.warn("Unknown signal {}", signal);
                break;
        }
    }

    private long forceKillTime() {
        return clock.millis() + killAfter;
    }

    private void forRunningConsumerProcess(Signal signal, java.util.function.Consumer<ConsumerProcess> consumerProcessConsumer) {
        if (consumerProcesses.hasRunningProcess(signal)) {
            consumerProcessConsumer.accept(consumerProcesses.getRunningProcess(signal));
        } else {
            metrics.counter("supervisor.signal.dropped." + signal.getType().name()).inc();
            logger.warn("Dropping signal {} as running target consumer process does not exist.", signal);
        }
    }

    private void forDyingConsumerProcess(Signal signal, java.util.function.Consumer<ConsumerProcess> consumerProcessConsumer) {
        if (consumerProcesses.hasDyingProcess(signal)) {
            consumerProcessConsumer.accept(consumerProcesses.getDyingProcess(signal));
        }
    }

    private void killRunning(Signal signal) {
        logger.info("Interrupting consumer process for subscription {}. {}", subscriptionName(signal), signal.getLogWithIdAndType());
        Future task = consumerProcesses.getRunningExecutionHandle(signal);
        consumerProcesses.moveToDyingProcesses(signal, clock.millis());
        if (task.cancel(true)) {
            logger.info("Interrupted consumer process {}. {}", subscriptionName(signal), signal.getLogWithIdAndType());
        } else {
            logger.error("Failed to interrupt consumer process {}, possible stale consumer. {}",
                    subscriptionName(signal), signal.getLogWithIdAndType());
        }
    }

    private void forceKillDying(Signal signal) {
        logger.info("Force interrupting already dying consumer process for subscription {}. {}", subscriptionName(signal), signal.getLogWithIdAndType());
        Future task = consumerProcesses.getDyingExecutionHandle(signal);
        if (!task.isDone()) {
            if (task.cancel(true)) {
                logger.info("Force interrupted consumer process {}. {}", subscriptionName(signal), signal.getLogWithIdAndType());
            } else {
                logger.error("Failed to force interrupt consumer process {}, possible stale consumer. {}",
                        subscriptionName(signal), signal.getLogWithIdAndType());
            }
        } else {
            logger.info("Consumer was already dead process {}. {}", signal.getTarget(), signal.getLogWithIdAndType());
            cleanup(signal);
        }
    }

    private void start(Signal start) {
        Subscription subscription = start.getPayload();

        if (!consumerProcesses.processExists(start)) {
            try {
                logger.info("Creating consumer for {}", subscription.getQualifiedName());
                ConsumerProcess process = createNewConsumerProcess(start);
                logger.info("Created consumer for {}. {}", subscription.getQualifiedName(), start.getLogWithIdAndType());

                logger.info("Starting consumer process for subscription {}. {}", start.getTarget(), start.getLogWithIdAndType());
                Future future = executor.execute(process);
                logger.info("Consumer for {} was added for execution. {}", subscription.getQualifiedName(), start.getLogWithIdAndType());

                consumerProcesses.add(process, future);
                logger.info("Started consumer process for subscription {}. {}", start.getTarget(), start.getLogWithIdAndType());
            } catch (Exception ex) {
                logger.error("Failed to create consumer for subscription {}", subscription.getQualifiedName(), ex);
            }
        } else if (consumerProcesses.hasDyingProcess(start)) {
            logger.info("Consumer process for {} is already dying, startup deferred.", subscription.getQualifiedName());
	        accept(start);
        } else {
            logger.info("Abort consumer process start: process for subscription {} is already running. {}",
                    start.getTarget(), start.getLogWithIdAndType());
        }
    }

    private void handleProcessShutdown(Signal lastSignal) {
        logger.info("Shutting down consumer process {}", lastSignal.getTarget());
        if (Thread.currentThread().isInterrupted()) {
            Signal cleanup = Signal.of(CLEANUP, subscriptionName(lastSignal));
            logger.info("Consumer process was interrupted. Its last processed signal is {}. Accepting {}", lastSignal, cleanup);
            accept(cleanup);
        } else {
            accept(lastSignal.createChild(CLEANUP));
        }
    }

    private void cleanup(Signal signal) {
        logger.info("Removing consumer process for subscription {}. {}", subscriptionName(signal), signal.getLogWithIdAndType());
        consumerProcesses.clean(signal);
    }

    private ConsumerProcess createNewConsumerProcess(Signal startSignal) {
        if (startSignal.getType() != START) {
            throw new IllegalArgumentException("Signal has to be START signal");
        }
        if (!(startSignal.getPayload() instanceof Subscription)) {
            throw new IllegalArgumentException("Signal's payload has to be Subscription type");
        }

        Consumer consumer = consumerFactory.createConsumer(startSignal.getPayload());
        return new ConsumerProcess(startSignal, consumer, retransmitter,
                this::handleProcessShutdown, clock, unhealthyAfter);
    }

    private SubscriptionName subscriptionName(ConsumerProcess consumerProcess) {
        return consumerProcess.getSubscription().getQualifiedName();
    }

    private SubscriptionName subscriptionName(Signal signal) {
        return signal.getTarget();
    }
}
