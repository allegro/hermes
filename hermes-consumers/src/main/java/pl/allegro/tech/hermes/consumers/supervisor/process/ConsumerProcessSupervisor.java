package pl.allegro.tech.hermes.consumers.supervisor.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.queue.MonitoredMpscQueue;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersExecutorService;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.KILL_UNHEALTHY;
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.RESTART;
import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.START;

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

    public ConsumerProcessSupervisor(ConsumersExecutorService executor,
                                     Retransmitter retransmitter,
                                     Clock clock,
                                     HermesMetrics metrics,
                                     ConfigFactory configs) {
        this.executor = executor;
        this.retransmitter = retransmitter;
        this.clock = clock;
        this.metrics = metrics;
        this.unhealthyAfter = configs.getIntProperty(Configs.CONSUMER_BACKGROUND_SUPERVISOR_UNHEALTHY_AFTER);
        this.killAfter = configs.getIntProperty(Configs.CONSUMER_BACKGROUND_SUPERVISOR_KILL_AFTER);
        this.taskQueue = new MonitoredMpscQueue<>(metrics, "signalQueue",
                configs.getIntProperty(Configs.CONSUMER_SIGNAL_PROCESSING_QUEUE_SIZE));
        this.signalsFilter = new SignalsFilter(taskQueue, clock);
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
                .filter(consumerProcess -> !isHealthy(consumerProcess))
                .forEach(consumerProcess -> taskQueue.offer(Signal.of(
                        Signal.SignalType.RESTART_UNHEALTHY, consumerProcess.getSubscriptionName())
                ));
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
                start(signal.getTarget(), signal.getPayload());
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
                    taskQueue.offer(Signal.of(Signal.SignalType.KILL, signal.getTarget(), killTime()));
                });
                break;
            case KILL:
                kill(signal.getTarget());
                break;
            case RESTART_UNHEALTHY:
                onConsumerProcess(signal, consumerProcess -> {
                    consumerProcess.accept(Signal.of(RESTART, signal.getTarget()));
                    taskQueue.offer(Signal.of(KILL_UNHEALTHY, signal.getTarget(), killTime()));
                });
                break;
            case KILL_UNHEALTHY:
                onConsumerProcess(signal, consumerProcess -> {
                    taskQueue.offer(Signal.of(START, signal.getTarget(), consumerProcess.getConsumer()));
                    kill(signal.getTarget());
                });
                break;
            case CLEANUP:
                cleanup(signal.getTarget());
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

    private void kill(SubscriptionName subscriptionName) {
        if (!runningProcesses.hasProcess(subscriptionName)) {
            logger.info("Process for subscription {} no longer exists", subscriptionName);
        } else {
            logger.info("Interrupting consumer process for subscription {}", subscriptionName);
            Future task = runningProcesses.getExecutionHandle(subscriptionName);
            if (!task.isDone()) {
                if (task.cancel(true)) {
                    logger.info("Interrupted consumer process {}", subscriptionName);
                } else {
                    logger.error("Failed to interrupt consumer process {}, possible stale consumer",
                            subscriptionName);
                }
            } else {
                runningProcesses.remove(subscriptionName);
                logger.info("Consumer was already dead process {}", subscriptionName);
            }
        }
    }

    private boolean isHealthy(ConsumerProcess consumerProcess) {
        long delta = clock.millis() - consumerProcess.healthcheckRefreshTime();
        if (delta > unhealthyAfter) {
            logger.info("Lost contact with consumer {}, last seen {}ms ago", consumerProcess, delta);
            return false;
        }
        return true;
    }

    private void start(SubscriptionName subscriptionName, Consumer consumer) {
        logger.info("Starting consumer process {}", subscriptionName);

        if (!runningProcesses.hasProcess(subscriptionName)) {
            ConsumerProcess process = new ConsumerProcess(subscriptionName, consumer, retransmitter,
                    this::handleProcessShutdown, clock);
            Future future = executor.execute(process);
            runningProcesses.add(process, future);
            logger.info("Started consumer process {}", process);
        } else {
            logger.info("Abort consumer process start: process for subscription {} is already running",
                    subscriptionName);
        }
    }

    private void handleProcessShutdown(SubscriptionName subscriptionName) {
        accept(Signal.of(Signal.SignalType.CLEANUP, subscriptionName));
    }

    private void cleanup(SubscriptionName subscriptionName) {
        runningProcesses.remove(subscriptionName);
    }

    public void shutdown() {
        runningProcesses.stream()
                .forEach(p -> p.accept(Signal.of(Signal.SignalType.STOP, p.getSubscriptionName())));
        executor.shutdown();
    }

    public List<RunningSubscriptionStatus> listRunningSubscriptions() {
        return runningProcesses.listRunningSubscriptions();
    }

    public Integer countRunningSubscriptions() {
        return runningProcesses.count();
    }
}
