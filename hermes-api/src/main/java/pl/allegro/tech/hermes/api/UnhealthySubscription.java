package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Set;

public class UnhealthySubscription {

  private final String name;
  private final String qualifiedTopicName;
  private final MonitoringDetails.Severity severity;
  private final Set<SubscriptionHealthProblem> problems;

  @JsonCreator
  public UnhealthySubscription(
      @JsonProperty("name") String name,
      @JsonProperty("topicName") String qualifiedTopicName,
      @JsonProperty("severity") MonitoringDetails.Severity severity,
      @JsonProperty("problems") Set<SubscriptionHealthProblem> problems) {
    this.name = name;
    this.qualifiedTopicName = qualifiedTopicName;
    this.severity = severity;
    this.problems = problems;
  }

  public static UnhealthySubscription from(
      Subscription subscription, SubscriptionHealth subscriptionHealth) {
    return new UnhealthySubscription(
        subscription.getName(),
        subscription.getQualifiedTopicName(),
        subscription.getMonitoringDetails().getSeverity(),
        subscriptionHealth.getProblems());
  }

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("topicName")
  public String getQualifiedTopicName() {
    return qualifiedTopicName;
  }

  @JsonProperty("severity")
  public MonitoringDetails.Severity getSeverity() {
    return severity;
  }

  @JsonProperty("problems")
  public Set<SubscriptionHealthProblem> getProblems() {
    return problems;
  }

  @Override
  public String toString() {
    return "UnhealthySubscription{"
        + "name='"
        + name
        + '\''
        + ", qualifiedTopicName='"
        + qualifiedTopicName
        + '\''
        + ", severity="
        + severity
        + ", problems="
        + problems
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UnhealthySubscription that = (UnhealthySubscription) o;
    return Objects.equals(name, that.name)
        && Objects.equals(qualifiedTopicName, that.qualifiedTopicName)
        && severity == that.severity
        && Objects.equals(problems, that.problems);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, qualifiedTopicName, severity, problems);
  }
}
