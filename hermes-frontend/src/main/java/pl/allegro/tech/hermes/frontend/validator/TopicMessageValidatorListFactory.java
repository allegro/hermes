package pl.allegro.tech.hermes.frontend.validator;

import com.google.common.collect.ImmutableList;
import org.glassfish.hk2.api.Factory;

import javax.inject.Inject;
import java.util.List;

public class TopicMessageValidatorListFactory implements Factory<List<TopicMessageValidator>> {

    private final AvroTopicMessageValidator avroTopicMessageValidator;
    private final JsonTopicMessageValidator jsonTopicMessageValidator;

    @Inject
    public TopicMessageValidatorListFactory(AvroTopicMessageValidator avroTopicMessageValidator, JsonTopicMessageValidator jsonTopicMessageValidator) {
        this.avroTopicMessageValidator = avroTopicMessageValidator;
        this.jsonTopicMessageValidator = jsonTopicMessageValidator;
    }

    @Override
    public List<TopicMessageValidator> provide() {
        return ImmutableList.of(avroTopicMessageValidator, jsonTopicMessageValidator);
    }

    @Override
    public void dispose(List<TopicMessageValidator> instance) {

    }

}
