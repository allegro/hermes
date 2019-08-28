package pl.allegro.tech.hermes.consumers.supervisor.workload.constraints;

public class Constraints {

    private final int consumersNumber;

    public Constraints(int consumersNumber) {
        this.consumersNumber = consumersNumber;
    }

    public int getConsumersNumber() {
        return consumersNumber;
    }
}
