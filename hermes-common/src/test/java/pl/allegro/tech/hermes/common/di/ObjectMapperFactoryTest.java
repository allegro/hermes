package pl.allegro.tech.hermes.common.di;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.common.di.factories.ObjectMapperFactory;

public class ObjectMapperFactoryTest {

  ObjectMapper mapper;

  @Before
  public void init() {
    ObjectMapperFactory factory = new ObjectMapperFactory(false);
    mapper = factory.provide();
  }

  @Test
  public void shouldDeserializeClassWithUnknownFields() throws Exception {
    // given
    String json = "{\"unknownProperty\": \"value\", \"name\":\"Charles\"}";

    // when
    DummyUser subscription = mapper.readValue(json, DummyUser.class);

    // then
    assertEquals("Charles", subscription.name);
  }

  @Test
  public void shouldSerializeObjectWithoutNullPropertiesInclusion() throws Exception {
    // given
    DummyUser object = new DummyUser(null);

    // when
    final String jsonValue = mapper.writeValueAsString(object);

    // then
    assertThat(jsonValue).doesNotContain("name").doesNotContain("null");
  }

  private record DummyUser(String name) {}
}
