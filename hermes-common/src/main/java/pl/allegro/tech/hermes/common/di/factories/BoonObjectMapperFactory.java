package pl.allegro.tech.hermes.common.di.factories;

import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.glassfish.hk2.api.Factory;

public class BoonObjectMapperFactory implements Factory<ObjectMapper> {

    @Override
    public ObjectMapper provide() {
        return JsonFactory.create();
    }

    @Override
    public void dispose(ObjectMapper instance) {

    }
}
