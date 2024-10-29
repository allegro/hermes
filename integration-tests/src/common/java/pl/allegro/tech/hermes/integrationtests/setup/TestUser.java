package pl.allegro.tech.hermes.integrationtests.setup;

import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;

class TestUser implements RequestUser {
  @Override
  public String getUsername() {
    return "test_user";
  }

  @Override
  public boolean isAdmin() {
    return true;
  }

  @Override
  public boolean isOwner(OwnerId ownerId) {
    return true;
  }
}
