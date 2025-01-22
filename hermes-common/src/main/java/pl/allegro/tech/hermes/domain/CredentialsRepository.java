package pl.allegro.tech.hermes.domain;

public interface CredentialsRepository {
  NodePassword readAdminPassword();

  void overwriteAdminPassword(String password);
}
