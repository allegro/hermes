package pl.allegro.tech.hermes.domain.filtering;

import java.util.List;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;

public interface MessageFilterSource {
  MessageFilter compile(MessageFilterSpecification specification);

  List<MessageFilter> getGlobalFilters();
}
