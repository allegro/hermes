package pl.allegro.tech.hermes.management.domain.dc;

public class DcBoundRepositoryHolder<Repository> {

    private final Repository repository;
    private final String dcName;

    public DcBoundRepositoryHolder(Repository repository, String dcName) {
        this.repository = repository;
        this.dcName = dcName;
    }

    public Repository getRepository() {
        return repository;
    }

    public String getDcName() {
        return dcName;
    }
}
