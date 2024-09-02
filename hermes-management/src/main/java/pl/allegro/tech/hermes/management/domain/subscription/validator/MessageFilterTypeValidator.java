package pl.allegro.tech.hermes.management.domain.subscription.validator;

import com.google.common.base.Objects;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;

class MessageFilterTypeValidator {

  private static final String ERROR_MESSAGE =
      "Message filter type %s doesn't match topic content type %s";

  private static final Set<ContentTypeFilterTypePair> VALID_TYPES_COMBINATIONS =
      new HashSet<>(
          Arrays.asList(
              new ContentTypeFilterTypePair(ContentType.JSON, "jsonpath"),
              new ContentTypeFilterTypePair(ContentType.JSON, "header"),
              new ContentTypeFilterTypePair(ContentType.AVRO, "avropath"),
              new ContentTypeFilterTypePair(ContentType.AVRO, "header")));

  void check(Subscription subscription, Topic topic) {
    subscription.getFilters().stream()
        .map(filter -> new ContentTypeFilterTypePair(topic.getContentType(), filter.getType()))
        .forEach(this::checkTypeMaching);
  }

  private void checkTypeMaching(ContentTypeFilterTypePair pair) {
    if (!VALID_TYPES_COMBINATIONS.contains(pair)) {
      throw new SubscriptionValidationException(
          String.format(ERROR_MESSAGE, pair.filterType, pair.contentType));
    }
  }

  static class ContentTypeFilterTypePair {
    private final ContentType contentType;
    private final String filterType;

    ContentTypeFilterTypePair(ContentType contentType, String filterType) {
      this.contentType = contentType;
      this.filterType = filterType;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ContentTypeFilterTypePair validPair = (ContentTypeFilterTypePair) o;
      return contentType == validPair.contentType
          && Objects.equal(filterType, validPair.filterType);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(contentType, filterType);
    }
  }
}
