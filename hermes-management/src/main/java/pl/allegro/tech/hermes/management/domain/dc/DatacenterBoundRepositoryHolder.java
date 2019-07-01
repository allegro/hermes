package pl.allegro.tech.hermes.management.domain.dc;

public class DatacenterBoundRepositoryHolder<Repository> {

    private final Repository repository;
    private final String datacenterName;

    public DatacenterBoundRepositoryHolder(Repository repository, String datacenterName) {
        this.repository = repository;
        this.datacenterName = datacenterName;
    }

    public Repository getRepository() {
        return repository;
    }

    public String getDatacenterName() {
        return datacenterName;
    }
}
