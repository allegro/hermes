package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.util.InetAddressInstanceIdResolver;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkBalancingParameters;

import java.time.Duration;

import static java.lang.Math.abs;
import static java.util.UUID.randomUUID;

@ConfigurationProperties(prefix = "consumer.workload")
public class WorkloadProperties implements WorkBalancingParameters {

    private int registryBinaryEncoderAssignmentsBufferSizeBytes = 100_000;

    private Duration rebalanceInterval = Duration.ofSeconds(30);

    private int consumersPerSubscription = 2;

    private int maxSubscriptionsPerConsumer = 200;

    private int assignmentProcessingThreadPoolSize = 5;

    private String nodeId = new InetAddressInstanceIdResolver().resolve().replaceAll("\\.", "_") + "$" + abs(randomUUID().getMostSignificantBits());

    private Duration monitorScanInterval = Duration.ofSeconds(120);

    private boolean autoRebalance = true;

    private Duration deadAfter = Duration.ofSeconds(120);

    public int getRegistryBinaryEncoderAssignmentsBufferSizeBytes() {
        return registryBinaryEncoderAssignmentsBufferSizeBytes;
    }

    public void setRegistryBinaryEncoderAssignmentsBufferSizeBytes(int registryBinaryEncoderAssignmentsBufferSizeBytes) {
        this.registryBinaryEncoderAssignmentsBufferSizeBytes = registryBinaryEncoderAssignmentsBufferSizeBytes;
    }

    @Override
    public Duration getRebalanceInterval() {
        return rebalanceInterval;
    }

    public void setRebalanceInterval(Duration rebalanceInterval) {
        this.rebalanceInterval = rebalanceInterval;
    }

    @Override
    public int getConsumersPerSubscription() {
        return consumersPerSubscription;
    }

    public void setConsumersPerSubscription(int consumersPerSubscription) {
        this.consumersPerSubscription = consumersPerSubscription;
    }

    @Override
    public int getMaxSubscriptionsPerConsumer() {
        return maxSubscriptionsPerConsumer;
    }

    public void setMaxSubscriptionsPerConsumer(int maxSubscriptionsPerConsumer) {
        this.maxSubscriptionsPerConsumer = maxSubscriptionsPerConsumer;
    }

    public int getAssignmentProcessingThreadPoolSize() {
        return assignmentProcessingThreadPoolSize;
    }

    public void setAssignmentProcessingThreadPoolSize(int assignmentProcessingThreadPoolSize) {
        this.assignmentProcessingThreadPoolSize = assignmentProcessingThreadPoolSize;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Duration getMonitorScanInterval() {
        return monitorScanInterval;
    }

    public void setMonitorScanInterval(Duration monitorScanInterval) {
        this.monitorScanInterval = monitorScanInterval;
    }

    @Override
    public boolean isAutoRebalance() {
        return autoRebalance;
    }

    public void setAutoRebalance(boolean autoRebalance) {
        this.autoRebalance = autoRebalance;
    }

    public Duration getDeadAfter() {
        return deadAfter;
    }

    public void setDeadAfter(Duration deadAfter) {
        this.deadAfter = deadAfter;
    }
}
