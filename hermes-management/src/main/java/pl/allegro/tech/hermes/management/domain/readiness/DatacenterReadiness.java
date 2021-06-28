package pl.allegro.tech.hermes.management.domain.readiness;

public class DatacenterReadiness {
    private final String datacenter;
    private final boolean ready;

    public DatacenterReadiness(String datacenter, boolean ready) {
        this.datacenter = datacenter;
        this.ready = ready;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public boolean isReady() {
        return ready;
    }

    @Override
    public String toString() {
        return "DatacenterReadiness{" +
                "datacenter='" + datacenter + '\'' +
                ", ready=" + ready +
                '}';
    }
}
