package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.util.InetAddressInstanceIdResolver;
import pl.allegro.tech.hermes.consumers.supervisor.workload.selective.SelectiveSupervisorParameters;

import java.time.Duration;

import static java.lang.Math.abs;
import static java.util.UUID.randomUUID;

@ConfigurationProperties(prefix = "consumer.workload")
public class WorkloadProperties {

    private int registryBinaryEncoderAssignmentsBufferSizeBytes = 100_000;

    private Duration rebalanceInterval = Duration.ofSeconds(30);

    private int consumerPerSubscription = 2;

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

    public Duration getRebalanceInterval() {
        return rebalanceInterval;
    }

    public void setRebalanceInterval(Duration rebalanceInterval) {
        this.rebalanceInterval = rebalanceInterval;
    }

    public int getConsumerPerSubscription() {
        return consumerPerSubscription;
    }

    public void setConsumerPerSubscription(int consumerPerSubscription) {
        this.consumerPerSubscription = consumerPerSubscription;
    }

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

    public SelectiveSupervisorParameters toSelectiveSupervisorParameters() {
        return new SelectiveSupervisorParameters(
                this.rebalanceInterval,
                this.consumerPerSubscription,
                this.maxSubscriptionsPerConsumer,
                this.nodeId,
                this.autoRebalance
        );
    }
}
