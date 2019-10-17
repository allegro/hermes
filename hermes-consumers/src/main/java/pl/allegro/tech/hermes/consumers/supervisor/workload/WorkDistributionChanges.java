package pl.allegro.tech.hermes.consumers.supervisor.workload;

import static java.lang.String.format;

public class WorkDistributionChanges {

    static final WorkDistributionChanges NO_CHANGES = new WorkDistributionChanges(0, 0);

    private final int assignmentsDeleted;
    private final int assignmentsCreated;

    WorkDistributionChanges(int assignmentsDeleted, int assignmentsCreated) {
        this.assignmentsDeleted = assignmentsDeleted;
        this.assignmentsCreated = assignmentsCreated;
    }

    public int getDeletedAssignmentsCount() {
        return assignmentsDeleted;
    }

    public int getCreatedAssignmentsCount() {
        return assignmentsCreated;
    }

    public String toString() {
        return format("assignments_created=%d, assignments_deleted=%d",
                assignmentsCreated, assignmentsDeleted);
    }
}
