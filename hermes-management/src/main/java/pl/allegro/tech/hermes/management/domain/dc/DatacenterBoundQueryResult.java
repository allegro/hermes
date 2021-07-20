package pl.allegro.tech.hermes.management.domain.dc;

public class DatacenterBoundQueryResult<T> {
    private final String datacenterName;
    private final T result;

    public DatacenterBoundQueryResult(T result, String datacenterName) {
        this.datacenterName = datacenterName;
        this.result = result;
    }

    public String getDatacenterName() {
        return datacenterName;
    }

    public T getResult() {
        return result;
    }

}
