package pl.allegro.tech.hermes.consumers.supervisor.process;

import org.jctools.queues.MpscArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersExecutorService;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class ConsumerProcessSupervisor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerProcessSupervisor.class);

    private final MpscArrayQueue<Signal> taskQueue = new MpscArrayQueue<>(1000);

    private final RunningConsumerProcesses runningProcesses = new RunningConsumerProcesses();

    private final Retransmitter retransmitter;

    private final ConsumersExecutorService executor;

    private final Clock clock;

    private final HermesMetrics metrics;

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
    }

    public void accept(Signal signal) {
        taskQueue.offer(signal);
    }

    @Override
    public void run() {
        logger.debug("Starting process supervisor loop");
        long currentTime = clock.millis();

        restartUnhealthy();

        List<Signal> generatedSignals = new ArrayList<>();
        taskQueue.drain(s -> processSignal(s, generatedSignals));
        generatedSignals.forEach(taskQueue::offer);

        logger.debug("Process supervisor loop took {} ms to check all consumers", clock.millis() - currentTime);
    }

    private void restartUnhealthy() {
        runningProcesses.stream()
                .filter(consumerProcess -> !isHealthy(consumerProcess))
                .forEach(consumerProcess -> taskQueue.offer(Signal.of(Signal.SignalType.RESTART_UNHEALTHY, consumerProcess.getSubscriptionName())));
    }

    private void processSignal(Signal signal, List<Signal> generatedSignals) {
        try {
            logger.debug("Processing signal: {}", signal);
            if (shouldSkipSignal(signal)) {
                logger.info("Skipping stale signal {} for subscription {}", signal.getType(), signal.getTarget());
                return;
            }

            if (!signal.canExecuteNow(clock.millis())) {
                generatedSignals.add(signal);
                return;
            }

            metrics.counter("supervisor.signal." + signal.getType().name()).inc();

            switch (signal.getType()) {
                case START:
                    start(signal.getTarget(), signal.getPayload());
                    break;
                case RETRANSMIT:
                    process(signal).accept(signal);
                    break;
                case UPDATE_SUBSCRIPTION:
                case UPDATE_TOPIC:
                    process(signal).accept(signal);
                    break;
                case RESTART:
                    process(signal).accept(signal);
                    break;
                case STOP:
                    process(signal).accept(signal);
                    generatedSignals.add(Signal.of(Signal.SignalType.KILL, signal.getTarget(), killTime()));
                    break;
                case KILL:
                    kill(signal.getTarget());
                    break;
                case RESTART_UNHEALTHY:
                    process(signal).accept(Signal.of(Signal.SignalType.STOP_RESTART, signal.getTarget()));
                    generatedSignals.add(Signal.of(Signal.SignalType.KILL_UNHEALTHY, signal.getTarget(), killTime()));
                    break;
                case KILL_UNHEALTHY:
                    Consumer consumer = runningProcesses.getProcess(signal.getTarget()).getConsumer();
                    generatedSignals.add(Signal.of(Signal.SignalType.START, signal.getTarget(), consumer));
                    kill(signal.getTarget());
                case CLEANUP:
                    cleanup(signal.getTarget());
                    break;
                default:
                    break;
            }
        } catch (Exception exception) {
            logger.error("Supervisor failed to process signal {}", signal, exception);
        }
    }

    private boolean shouldSkipSignal(Signal signal) {
        return signal.getType() != Signal.SignalType.START && !runningProcesses.hasProcess(signal.getTarget());
    }

    private long killTime() {
        return clock.millis() + killAfter;
    }

    private ConsumerProcess process(Signal signal) {
        return runningProcesses.getProcess(signal.getTarget());
    }

    private void kill(SubscriptionName subscriptionName) {
        if (runningProcesses.hasProcess(subscriptionName)) {
            logger.info("Process for subscription {} no longer exists", subscriptionName);
        } else {
            logger.info("Interrupting consumer process for subscription {}", subscriptionName);
            Future task = runningProcesses.getExecutionHandle(subscriptionName);
            runningProcesses.remove(subscriptionName);
            try {
                if (!task.isDone()) {
                    if (task.cancel(true)) {
                        logger.info("Interrupted consumer process {}", subscriptionName);
                    } else {
                        logger.error("Failed to interrupt consumer process {}, possible stale consumer", subscriptionName);
                    }
                } else {
                    logger.info("Consumer was already dead process {}", subscriptionName);
                }
            } finally {
                cleanup(subscriptionName);
            }
        }
    }

    private boolean isHealthy(ConsumerProcess consumerProcess) {
        long delta = clock.millis() - consumerProcess.healtcheckRefreshTime();
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
            logger.info("Abort consumer process start: process for subscription {} is already running", subscriptionName);
        }
    }

    private void handleProcessShutdown(SubscriptionName subscriptionName, Signal.SignalType reason) {
        Consumer consumer = runningProcesses.getProcess(subscriptionName).getConsumer();
        accept(Signal.of(Signal.SignalType.CLEANUP, subscriptionName));
        if (reason == Signal.SignalType.STOP_RESTART) {
            accept(Signal.of(Signal.SignalType.START, subscriptionName, consumer));
        }
    }

    private void cleanup(SubscriptionName subscriptionName) {
        runningProcesses.remove(subscriptionName);
    }

    public void shutdown() {
        runningProcesses.stream().forEach(p -> p.accept(Signal.of(Signal.SignalType.STOP, p.getSubscriptionName())));
        executor.shutdown();
    }
}
