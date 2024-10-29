package pl.allegro.tech.hermes.consumers.config;

import static java.lang.Math.abs;
import static java.util.UUID.randomUUID;

import java.time.Duration;
import java.util.Arrays;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.util.InetAddressInstanceIdResolver;
import pl.allegro.tech.hermes.consumers.supervisor.workload.WorkBalancingParameters;

@ConfigurationProperties(prefix = "consumer.workload")
public class WorkloadProperties implements WorkBalancingParameters {

  private int registryBinaryEncoderAssignmentsBufferSizeBytes = 100_000;

  private Duration rebalanceInterval = Duration.ofSeconds(30);

  private int consumersPerSubscription = 2;

  private int maxSubscriptionsPerConsumer = 200;

  private int assignmentProcessingThreadPoolSize = 5;

  private String nodeId =
      new InetAddressInstanceIdResolver().resolve().replaceAll("\\.", "_")
          + "$"
          + abs(randomUUID().getMostSignificantBits());

  private Duration monitorScanInterval = Duration.ofSeconds(120);

  private boolean autoRebalance = true;

  private Duration deadAfter = Duration.ofSeconds(120);

  private WorkBalancingStrategy workBalancingStrategy = WorkBalancingStrategy.SELECTIVE;

  @NestedConfigurationProperty
  private WeightedWorkBalancingProperties weightedWorkBalancing =
      new WeightedWorkBalancingProperties();

  public int getRegistryBinaryEncoderAssignmentsBufferSizeBytes() {
    return registryBinaryEncoderAssignmentsBufferSizeBytes;
  }

  public void setRegistryBinaryEncoderAssignmentsBufferSizeBytes(
      int registryBinaryEncoderAssignmentsBufferSizeBytes) {
    this.registryBinaryEncoderAssignmentsBufferSizeBytes =
        registryBinaryEncoderAssignmentsBufferSizeBytes;
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

  public WorkBalancingStrategy getWorkBalancingStrategy() {
    return workBalancingStrategy;
  }

  public void setWorkBalancingStrategy(WorkBalancingStrategy workBalancingStrategy) {
    this.workBalancingStrategy = workBalancingStrategy;
  }

  public WeightedWorkBalancingProperties getWeightedWorkBalancing() {
    return weightedWorkBalancing;
  }

  public void setWeightedWorkBalancing(WeightedWorkBalancingProperties weightedWorkBalancing) {
    this.weightedWorkBalancing = weightedWorkBalancing;
  }

  public static class WeightedWorkBalancingProperties {

    private int consumerLoadEncoderBufferSizeBytes = 100_000;

    private int subscriptionProfilesEncoderBufferSizeBytes = 100_000;

    private Duration loadReportingInterval = Duration.ofMinutes(1);

    private Duration stabilizationWindowSize = Duration.ofMinutes(30);

    private double minSignificantChangePercent = 5;

    private Duration weightWindowSize = Duration.ofMinutes(15);

    private TargetWeightCalculationStrategy targetWeightCalculationStrategy =
        TargetWeightCalculationStrategy.AVG;

    private double scoringGain = 1.0d;

    public int getConsumerLoadEncoderBufferSizeBytes() {
      return consumerLoadEncoderBufferSizeBytes;
    }

    public void setConsumerLoadEncoderBufferSizeBytes(int consumerLoadEncoderBufferSizeBytes) {
      this.consumerLoadEncoderBufferSizeBytes = consumerLoadEncoderBufferSizeBytes;
    }

    public int getSubscriptionProfilesEncoderBufferSizeBytes() {
      return subscriptionProfilesEncoderBufferSizeBytes;
    }

    public void setSubscriptionProfilesEncoderBufferSizeBytes(
        int subscriptionProfilesEncoderBufferSizeBytes) {
      this.subscriptionProfilesEncoderBufferSizeBytes = subscriptionProfilesEncoderBufferSizeBytes;
    }

    public Duration getLoadReportingInterval() {
      return loadReportingInterval;
    }

    public void setLoadReportingInterval(Duration loadReportingInterval) {
      this.loadReportingInterval = loadReportingInterval;
    }

    public Duration getStabilizationWindowSize() {
      return stabilizationWindowSize;
    }

    public void setStabilizationWindowSize(Duration stabilizationWindowSize) {
      this.stabilizationWindowSize = stabilizationWindowSize;
    }

    public double getMinSignificantChangePercent() {
      return minSignificantChangePercent;
    }

    public void setMinSignificantChangePercent(double minSignificantChangePercent) {
      this.minSignificantChangePercent = minSignificantChangePercent;
    }

    public Duration getWeightWindowSize() {
      return weightWindowSize;
    }

    public void setWeightWindowSize(Duration weightWindowSize) {
      this.weightWindowSize = weightWindowSize;
    }

    public TargetWeightCalculationStrategy getTargetWeightCalculationStrategy() {
      return targetWeightCalculationStrategy;
    }

    public void setTargetWeightCalculationStrategy(
        TargetWeightCalculationStrategy targetWeightCalculationStrategy) {
      this.targetWeightCalculationStrategy = targetWeightCalculationStrategy;
    }

    public double getScoringGain() {
      return scoringGain;
    }

    public void setScoringGain(double scoringGain) {
      this.scoringGain = scoringGain;
    }
  }

  public enum WorkBalancingStrategy {
    SELECTIVE,
    WEIGHTED;

    public static class UnknownWorkBalancingStrategyException extends InternalProcessingException {

      public UnknownWorkBalancingStrategyException() {
        super("Unknown work balancing strategy. Use one of: " + Arrays.toString(values()));
      }
    }
  }

  public enum TargetWeightCalculationStrategy {
    AVG,
    SCORING;

    public static class UnknownTargetWeightCalculationStrategyException
        extends InternalProcessingException {

      public UnknownTargetWeightCalculationStrategyException() {
        super(
            "Unknown target weight calculation strategy. Use one of: " + Arrays.toString(values()));
      }
    }
  }
}
