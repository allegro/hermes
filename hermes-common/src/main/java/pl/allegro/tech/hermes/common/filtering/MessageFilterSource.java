package pl.allegro.tech.hermes.common.filtering;

import pl.allegro.tech.hermes.api.MessageFilterSpecification;

import java.util.List;

public interface MessageFilterSource {
    MessageFilter compile(MessageFilterSpecification specification);
    List<MessageFilter> getGlobalFilters();
}
