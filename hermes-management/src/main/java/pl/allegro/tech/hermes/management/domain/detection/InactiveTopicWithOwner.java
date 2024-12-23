package pl.allegro.tech.hermes.management.domain.detection;

import pl.allegro.tech.hermes.api.OwnerId;

public record InactiveTopicWithOwner(InactiveTopic topic, OwnerId ownerId) {}
