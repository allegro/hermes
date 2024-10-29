package pl.allegro.tech.hermes.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TopicNameTest {

  @Test
  public void shouldConvertQualifiedName() {
    // given
    TopicName topicName = new TopicName("group1", "topic1");

    // when & then
    assertThat(TopicName.fromQualifiedName(topicName.qualifiedName())).isEqualTo(topicName);
  }

  @Test
  public void shouldHandleDottedGroupName() {
    // given
    TopicName topicName = TopicName.fromQualifiedName("group.topic");

    // when & then
    assertThat(topicName.getGroupName()).isEqualTo("group");
    assertThat(topicName.getName()).isEqualTo("topic");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionWhenInvalidQualifiedNameProvided() {
    // when & then
    TopicName.fromQualifiedName("invalidQualifiedName");
  }

  @Test
  public void shouldBeNullSafe() {
    // when & then
    assertThat(TopicName.fromQualifiedName(null)).isNull();
  }
}
