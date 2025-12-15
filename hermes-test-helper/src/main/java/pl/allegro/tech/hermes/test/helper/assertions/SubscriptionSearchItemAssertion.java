package pl.allegro.tech.hermes.test.helper.assertions;

import org.assertj.core.api.Assertions;
import pl.allegro.tech.hermes.api.SearchItemType;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionSearchItem;

@SuppressWarnings("UnusedReturnValue")
public class SubscriptionSearchItemAssertion
    extends SearchItemAssertion<SubscriptionSearchItemAssertion, SubscriptionSearchItem> {
  private SubscriptionSearchItemAssertion(SubscriptionSearchItem actual) {
    super(actual, SubscriptionSearchItemAssertion.class);
  }

  public SubscriptionSearchItemAssertion hasPropertiesOfSubscription(
      Subscription expectedSubscription) {
    return hasName(expectedSubscription.getName())
        .hasType(SearchItemType.SUBSCRIPTION)
        .hasTopicQualifiedName(expectedSubscription.getTopicName().qualifiedName())
        .hasTopicGroupName(expectedSubscription.getTopicName().getGroupName())
        .hasEndpoint(expectedSubscription.getEndpoint().getEndpoint());
  }

  public SubscriptionSearchItemAssertion hasTopicQualifiedName(String expectedTopicQualifiedName) {
    Assertions.assertThat(actual.subscription().topic().qualifiedName())
        .isEqualTo(expectedTopicQualifiedName);
    return this;
  }

  public SubscriptionSearchItemAssertion hasTopicGroupName(String expectedTopicGroupName) {
    Assertions.assertThat(actual.subscription().topic().groupName())
        .isEqualTo(expectedTopicGroupName);
    return this;
  }

  public SubscriptionSearchItemAssertion hasEndpoint(String expectedEndpoint) {
    Assertions.assertThat(actual.subscription().endpoint()).isEqualTo(expectedEndpoint);
    return this;
  }

  public static SubscriptionSearchItemAssertion assertThat(SubscriptionSearchItem actual) {
    return new SubscriptionSearchItemAssertion(actual);
  }
}
