package pl.allegro.tech.hermes.consumers.supervisor.monitor;

import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SupervisorController;

import javax.inject.Inject;

public class ConsumersRuntimeMonitorFactory implements Factory<ConsumersRuntimeMonitor> {

    private static final Logger logger = LoggerFactory.getLogger(ConsumersRuntimeMonitorFactory.class);

    private final ConsumersRuntimeMonitor monitor;

    @Inject
    public ConsumersRuntimeMonitorFactory(
            ConsumersSupervisor consumerSupervisor,
            SupervisorController workloadSupervisor,
            HermesMetrics hermesMetrics,
            ConfigFactory configFactory
    ) {
        monitor = new ConsumersRuntimeMonitor(consumerSupervisor, workloadSupervisor, hermesMetrics, configFactory);
    }

    @Override
    public ConsumersRuntimeMonitor provide() {
        return monitor;
    }

    @Override
    public void dispose(ConsumersRuntimeMonitor instance) {
        try {
            monitor.shutdown();
        } catch (InterruptedException exception) {
            logger.warn("Got exception when stopping consumers runtime monitor", exception);
        }
    }
}
