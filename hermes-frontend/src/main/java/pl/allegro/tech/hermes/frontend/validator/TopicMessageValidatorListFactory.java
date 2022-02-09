package pl.allegro.tech.hermes.frontend.validator;

import com.google.common.collect.ImmutableList;
import org.glassfish.hk2.api.Factory;

import javax.inject.Inject;
import java.util.List;

@Deprecated
public class TopicMessageValidatorListFactory implements Factory<List<TopicMessageValidator>> { //TODO - remove?

    private final Iterable<TopicMessageValidator> topicMessageValidators;

    @Inject
    public TopicMessageValidatorListFactory(Iterable<TopicMessageValidator> topicMessageValidators) {
        this.topicMessageValidators = topicMessageValidators;
    }

    @Override
    public List<TopicMessageValidator> provide() {
        return ImmutableList.copyOf(topicMessageValidators);
    }

    @Override
    public void dispose(List<TopicMessageValidator> instance) {

    }

}
