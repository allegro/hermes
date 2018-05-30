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

    private final RunningConsumerProcesses runningProcesses = new RunningConsumerProcesses();

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
    }

    public void accept(Signal signal) {
        taskQueue.offer(signal);
    }

    public Set<SubscriptionName> existingConsumers() {
        return runningProcesses.existingConsumers();
    }

    @Override
    public void run() {
        logger.debug("Starting process supervisor loop");
        long currentTime = clock.millis();

        restartUnhealthy();

        List<Signal> signalsToProcess = new ArrayList<>();
        taskQueue.drain(signalsToProcess::add);
        signalsFilter.filterSignals(signalsToProcess, runningProcesses.existingConsumers())
                .forEach(this::tryToProcessSignal);

        logger.debug("Process supervisor loop took {} ms to check all consumers", clock.millis() - currentTime);
    }

    private void restartUnhealthy() {
        runningProcesses.stream()
                .filter(consumerProcess -> !consumerProcess.isHealthy())
                .forEach(consumerProcess -> {
                    Signal restartUnhealthy =
                            Signal.of(RESTART_UNHEALTHY, consumerProcess.getSubscription().getQualifiedName());
                    logger.info("Lost contact with consumer {}, last seen {}ms ago {}. {}",
                            consumerProcess, consumerProcess.lastSeen(), restartUnhealthy.getLogWithIdAndType());
                    taskQueue.offer(restartUnhealthy);
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
                onConsumerProcess(signal, consumerProcess -> consumerProcess.accept(signal));
                break;
            case STOP:
                onConsumerProcess(signal, consumerProcess -> {
                    consumerProcess.accept(signal);
                    taskQueue.offer(signal.createChild(KILL, killTime()));
                });
                break;
            case KILL:
                kill(signal);
                break;
            case RESTART_UNHEALTHY:
                onConsumerProcess(signal, consumerProcess -> {
                    consumerProcess.accept(signal.createChild(RESTART));
                    taskQueue.offer(signal.createChild(KILL_UNHEALTHY, killTime()));
                });
                break;
            case KILL_UNHEALTHY:
                onConsumerProcess(signal, consumerProcess -> {
                    taskQueue.offer(signal.createChild(START, clock.millis(), consumerProcess.getSubscription()));
                    kill(signal);
                });
                break;
            case CLEANUP:
                cleanup(signal);
                break;
            default:
                logger.warn("Unknown signal {}", signal);
                break;
        }
    }

    private long killTime() {
        return clock.millis() + killAfter;
    }

    private void onConsumerProcess(Signal signal, java.util.function.Consumer<ConsumerProcess> consumerProcessConsumer) {
        if (runningProcesses.hasProcess(signal.getTarget())) {
            consumerProcessConsumer.accept(runningProcesses.getProcess(signal.getTarget()));
        } else {
            metrics.counter("supervisor.signal.dropped." + signal.getType().name()).inc();
            logger.warn("Dropping signal {} as target consumer process does not exist.", signal);
        }
    }

    private void kill(Signal signal) {
        if (!runningProcesses.hasProcess(signal.getTarget())) {
            logger.info("Process for subscription {} no longer exists. {}", signal.getTarget(), signal.getLogWithIdAndType());
        } else {
            logger.info("Interrupting consumer process for subscription {}. {}", signal.getTarget(), signal.getLogWithIdAndType());
            Future task = runningProcesses.getExecutionHandle(signal.getTarget());
            if (!task.isDone()) {
                if (task.cancel(true)) {
                    logger.info("Interrupted consumer process {}. {}", signal.getTarget(), signal.getLogWithIdAndType());
                } else {
                    logger.error("Failed to interrupt consumer process {}, possible stale consumer. {}",
                            signal.getTarget(), signal.getLogWithIdAndType());
                }
            } else {
                runningProcesses.remove(signal.getTarget());
                logger.info("Consumer was already dead process {}. {}", signal.getTarget(), signal.getLogWithIdAndType());
            }
        }
    }

    private void start(Signal start) {
        Subscription subscription = start.getPayload();

        if (!runningProcesses.hasProcess(start.getTarget())) {
            try {
                logger.info("Creating consumer for {}", subscription.getQualifiedName());
                ConsumerProcess process = createNewConsumerProcess(start);
                logger.info("Created consumer for {}. {}", subscription.getQualifiedName(), start.getLogWithIdAndType());

                logger.info("Starting consumer process for subscription {}. {}", start.getTarget(), start.getLogWithIdAndType());
                Future future = executor.execute(process);
                logger.info("Consumer for {} was added for execution. {}", subscription.getQualifiedName(), start.getLogWithIdAndType());

                runningProcesses.add(process, future);
                logger.info("Started consumer process for subscription {}. {}", start.getTarget(), start.getLogWithIdAndType());
            } catch (Exception ex) {
                logger.error("Failed to create consumer for subscription {}", subscription.getQualifiedName(), ex);
            }
        } else {
            logger.info("Abort consumer process start: process for subscription {} is already running. {}",
                    start.getTarget(), start.getLogWithIdAndType());
        }
    }

    private void handleProcessShutdown(Signal lastSignal) {
        if (Thread.interrupted()) {
            Signal cleanup = Signal.of(CLEANUP, lastSignal.getTarget());
            logger.info("Consumer process was interrupted. Its last processed signal is {}. Accepting {}", lastSignal, cleanup);
            accept(cleanup);
        } else {
            accept(lastSignal.createChild(CLEANUP));
        }
    }

    private void cleanup(Signal signal) {
        logger.info("Removing consumer process for subscription {}. {}", signal.getTarget(), signal.getLogWithIdAndType());
        runningProcesses.remove(signal.getTarget());
    }

    public void shutdown() {
        runningProcesses.stream()
                .forEach(p -> p.accept(Signal.of(STOP, p.getSubscription().getQualifiedName())));
        executor.shutdown();
    }

    public List<RunningSubscriptionStatus> listRunningSubscriptions() {
        return runningProcesses.listRunningSubscriptions();
    }

    public Integer countRunningSubscriptions() {
        return runningProcesses.count();
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
}
