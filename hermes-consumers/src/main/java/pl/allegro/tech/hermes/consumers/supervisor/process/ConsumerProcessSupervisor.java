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
import java.util.Optional;
import java.util.concurrent.Future;

public class ConsumerProcessSupervisor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerProcessSupervisor.class);

    private final MpscArrayQueue<Signal> taskQueue = new MpscArrayQueue<>(1000);

    private final RunningConsumerProcesses runningProcesses = new RunningConsumerProcesses();

    private final Retransmitter retransmitter;

    private final ConsumersExecutorService executor;

    private final Clock clock;

    private final HermesMetrics metrics;

    private long unhealthyAfter;

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
    }

    public void accept(Signal signal) {
        taskQueue.offer(signal);
    }

    @Override
    public void run() {
        logger.info("Starting process supervisor loop");
        long currentTime = clock.millis();

        restartUnhealthy();
        taskQueue.drain(this::processSignal);

        logger.info("Process supervisor loop took {} ms to check all consumers", clock.millis() - currentTime);
    }

    private void restartUnhealthy() {
        runningProcesses.stream()
                .filter(consumerProcess -> !isHealthy(consumerProcess))
                .forEach(consumerProcess -> taskQueue.offer(Signal.of(Signal.SignalType.KILL_RESTART, consumerProcess.getSubscriptionName())));
    }

    private void processSignal(Signal signal) {
        try {
            logger.debug("Processing signal: {}", signal);
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
                case KILL_RESTART:
                    kill(process(signal));
                    taskQueue.offer(Signal.of(Signal.SignalType.START, signal.getTarget()));
                    break;
                case STOP:
                    process(signal).accept(signal);
                    taskQueue.offer(Signal.of(Signal.SignalType.CLEANUP, signal.getTarget()));
                    break;
                case CLEANUP:
                    cleanup(process(signal));
                    break;
                default:
                    break;
            }
        } catch (Exception exception) {
            logger.error("Supervisor failed to process signal {}", signal, exception);
        }
    }

    private ConsumerProcess process(Signal signal) {
        return runningProcesses.getProcess(signal.getTarget());
    }

    private void kill(ConsumerProcess consumerProcess) {
        logger.info("Interrupting consumer process {}", consumerProcess);
        Future task = runningProcesses.getExecutionHandle(consumerProcess);
        runningProcesses.remove(consumerProcess);
        if (!task.isDone()) {
            if (task.cancel(true)) {
                logger.info("Interrupted consumer process {}", consumerProcess);
            } else {
                logger.error("Failed to interrupt consumer process {}, possible stale consumer", consumerProcess);
            }
        } else {
            logger.info("Consumer was already dead process {}", consumerProcess);
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

    private void start(SubscriptionName subscriptionName, Optional<Consumer> consumer) {
        logger.info("Starting consumer process {}", subscriptionName);

        ConsumerProcess process;
        if (consumer.isPresent()) {
            process = new ConsumerProcess(subscriptionName, consumer.get(), retransmitter, clock);
        } else if (runningProcesses.hasProcess(subscriptionName)) {
            process = runningProcesses.getProcess(subscriptionName);
        } else {
            logger.error("Failed to start: no consumer process for {} and none provided", subscriptionName);
            return;
        }

        Future future = executor.execute(process);
        runningProcesses.add(process, future);

        logger.info("Started consumer process {}", process);
    }

    private void cleanup(ConsumerProcess consumerProcess) {
        kill(consumerProcess);
    }

    public void shutdown() {
        runningProcesses.stream().forEach(p -> p.accept(Signal.of(Signal.SignalType.STOP, p.getSubscriptionName())));
        executor.shutdown();
    }
}
