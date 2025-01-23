package pl.allegro.tech.hermes.management.domain.dc;

public class DatacenterBoundRepositoryHolder<R> {

  private final R repository;
  private final String datacenterName;

  public DatacenterBoundRepositoryHolder(R repository, String datacenterName) {
    this.repository = repository;
    this.datacenterName = datacenterName;
  }

  public R getRepository() {
    return repository;
  }

  public String getDatacenterName() {
    return datacenterName;
  }
}
