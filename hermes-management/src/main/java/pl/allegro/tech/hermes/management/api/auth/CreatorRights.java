package pl.allegro.tech.hermes.management.api.auth;

public interface CreatorRights<T> {

  boolean allowedToManage(T entity);

  boolean allowedToCreate(T entity);
}
