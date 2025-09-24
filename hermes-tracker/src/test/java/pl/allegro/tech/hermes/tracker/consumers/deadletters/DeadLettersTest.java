package pl.allegro.tech.hermes.tracker.consumers.deadletters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder;

public class DeadLettersTest {

  @Test
  public void testDeadLetterswhenEmpty() {
    // given
    Subscription subscription =
        SubscriptionBuilder.subscription("pl.allegro.group.topic", "subscription").build();
    DeadMessage message = DeadMessageBuilder.getRandomDeadMessage();
    DeadLetters deadLetters = new DeadLetters(List.of());

    // then
    Assertions.assertThatCode(() -> deadLetters.send(subscription, message)).doesNotThrowAnyException();
  }

  @Test
  public void testDedadLetterswhenRepoSupports() {
    // given
    Subscription subscription =
        SubscriptionBuilder.subscription("pl.allegro.group.topic", "subscription").build();
    DeadMessage message = DeadMessageBuilder.getRandomDeadMessage();
    TestDeadRepository repository = TestDeadRepository.SUPPORTING();
    DeadLetters deadLetters = new DeadLetters(List.of(repository));

    // when
    deadLetters.send(subscription, message);

    // then
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  public void testDeadLetterswhenRepoDoesNotSupport() {
    // given
    Subscription subscription =
        SubscriptionBuilder.subscription("pl.allegro.group.topic", "subscription").build();
    DeadMessage message = DeadMessageBuilder.getRandomDeadMessage();
    TestDeadRepository repository = TestDeadRepository.UNSUPPORTING();
    DeadLetters deadLetters = new DeadLetters(List.of(repository));

    // when
    deadLetters.send(subscription, message);

    // then
    assertThat(repository.count()).isEqualTo(0);
  }

  @Test
  public void testDeadLetterswhenRepoAreMixed() {
    // given
    Subscription subscription =
        SubscriptionBuilder.subscription("pl.allegro.group.topic", "subscription").build();
    DeadMessage message = DeadMessageBuilder.getRandomDeadMessage();
    TestDeadRepository supportingRepository = TestDeadRepository.SUPPORTING();
    TestDeadRepository notSupportingRepository = TestDeadRepository.UNSUPPORTING();
    DeadLetters deadLetters =
        new DeadLetters(List.of(supportingRepository, notSupportingRepository));

    // when
    deadLetters.send(subscription, message);

    // then
    assertThat(supportingRepository.count()).isEqualTo(1);
    assertThat(notSupportingRepository.isEmpty()).isTrue();
  }
}
