package pl.allegro.tech.hermes.consumers.supervisor.workload;

public class WorkBalancingResult {

  private final SubscriptionAssignmentView state;
  private final int missingResources;

  public WorkBalancingResult(SubscriptionAssignmentView balancedState, int missingResources) {
    this.state = balancedState;
    this.missingResources = missingResources;
  }

  public SubscriptionAssignmentView getAssignmentsView() {
    return state;
  }

  public int getMissingResources() {
    return missingResources;
  }
}
