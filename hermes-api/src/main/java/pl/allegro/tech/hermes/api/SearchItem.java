package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = TopicSearchItem.class, name = "TOPIC"),
  @JsonSubTypes.Type(value = SubscriptionSearchItem.class, name = "SUBSCRIPTION"),
})
public interface SearchItem {
  SearchItemType type();

  String name();
}
