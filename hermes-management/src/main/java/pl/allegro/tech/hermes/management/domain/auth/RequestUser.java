package pl.allegro.tech.hermes.management.domain.auth;

import pl.allegro.tech.hermes.api.OwnerId;

public interface RequestUser {

  String getUsername();

  boolean isAdmin();

  boolean isOwner(OwnerId ownerId);
}
