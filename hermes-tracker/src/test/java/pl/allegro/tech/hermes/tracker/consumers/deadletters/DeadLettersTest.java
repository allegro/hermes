package pl.allegro.tech.hermes.tracker.consumers.deadletters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;

public class DeadLettersTest {

  @Test
  public void testDeadLettersWhenEmpty() {
    // Given
    Subscription subscription =
        SubscriptionBuilder.subscription("pl.allegro.group.topic", "subscription").build();
    DeadMessage message = DeadMessageBuilder.getRandomDeadMessage();
    DeadLetters deadLetters = new DeadLetters(List.of());

    // When
    deadLetters.send(subscription, message);

    // Then
    // No exception should be thrown
  }

  @Test
  public void testDedadLettersWhenRepoSupports() {
    // Given
    Subscription subscription =
        SubscriptionBuilder.subscription("pl.allegro.group.topic", "subscription").build();
    DeadMessage message = DeadMessageBuilder.getRandomDeadMessage();
    TestDeadRepository repository = TestDeadRepository.SUPPORTING();
    DeadLetters deadLetters = new DeadLetters(List.of(repository));

    // When
    deadLetters.send(subscription, message);

    // Then
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  public void testDeadLettersWhenRepoDoesNotSupport() {
    // Given
    Subscription subscription =
        SubscriptionBuilder.subscription("pl.allegro.group.topic", "subscription").build();
    DeadMessage message = DeadMessageBuilder.getRandomDeadMessage();
    TestDeadRepository repository = TestDeadRepository.UNSUPPORTING();
    DeadLetters deadLetters = new DeadLetters(List.of(repository));

    // When
    deadLetters.send(subscription, message);

    // Then
    assertThat(repository.count()).isEqualTo(0);
  }

  @Test
  public void testDeadLettersWhenRepoAreMixed() {
    // Given
    Subscription subscription =
        SubscriptionBuilder.subscription("pl.allegro.group.topic", "subscription").build();
    DeadMessage message = DeadMessageBuilder.getRandomDeadMessage();
    TestDeadRepository supportingRepository = TestDeadRepository.SUPPORTING();
    TestDeadRepository notSupportingRepository = TestDeadRepository.UNSUPPORTING();
    DeadLetters deadLetters =
        new DeadLetters(List.of(supportingRepository, notSupportingRepository));

    // When
    deadLetters.send(subscription, message);

    // Then
    assertThat(supportingRepository.count()).isEqualTo(1);
    assertThat(notSupportingRepository.isEmpty()).isTrue();
  }
}
