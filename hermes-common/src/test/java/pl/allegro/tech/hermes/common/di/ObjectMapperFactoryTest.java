package pl.allegro.tech.hermes.common.di;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.config.DynamicPropertyFactory;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.di.factories.ObjectMapperFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class ObjectMapperFactoryTest {

    ObjectMapper mapper;

    @Before
    public void init() {
        ObjectMapperFactory factory = new ObjectMapperFactory(new ConfigFactory(DynamicPropertyFactory.getInstance()));
        mapper = factory.provide();
    }

    @Test
    public void shouldDeserializeClassWithUnknownFields() throws Exception {
        //given
        String json = "{\"unknownProperty\": \"value\", \"name\":\"Charles\"}";

        //when
        DummyUser subscription = mapper.readValue(json, DummyUser.class);

        //then
        assertEquals("Charles", subscription.name);
    }

    @Test
    public void shouldSerializeObjectWithoutNullPropertiesInclusion() throws Exception {
        //given
        DummyUser object = new DummyUser(null);

        //when
        final String jsonValue = mapper.writeValueAsString(object);

        //then
        assertThat(jsonValue).doesNotContain("name").doesNotContain("null");
    }

    private static final class DummyUser {
        private String name;

        private DummyUser() {
        }

        private DummyUser(String name) {
            this.name = name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
