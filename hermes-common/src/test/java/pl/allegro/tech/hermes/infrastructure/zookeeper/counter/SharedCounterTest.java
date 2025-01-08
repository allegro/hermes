package pl.allegro.tech.hermes.infrastructure.zookeeper.counter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

public class SharedCounterTest extends ZookeeperBaseTest {

  private SharedCounter counter;

  @Before
  public void initialize() {
    this.counter =
        new SharedCounter(zookeeperClient, Duration.ofHours(72), Duration.ofSeconds(1), 3);
  }

  @Test
  public void shouldIncrementAndRetrieveCounterForGivenPath() {
    // given when
    counter.increment("/increment", 10);
    wait.untilZookeeperPathIsCreated("/increment");

    // then
    assertThat(counter.getValue("/increment")).isEqualTo(10);
  }

  @Test
  public void shouldIncrementCounterAtomicallyWhenIncrementedConcurrently() {
    // given
    SharedCounter otherCounter =
        new SharedCounter(zookeeperClient, Duration.ofHours(72), Duration.ofSeconds(1), 3);

    // when
    counter.increment("/sharedIncrement", 10);
    otherCounter.increment("/sharedIncrement", 15);
    wait.untilZookeeperPathIsCreated("/sharedIncrement");

    // then
    assertThat(counter.getValue("/sharedIncrement")).isEqualTo(25);
  }
}
