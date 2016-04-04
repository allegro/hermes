package pl.allegro.tech.hermes.consumers.consumer.filtering.json;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.filtering.SubscriptionMessageFilterCompiler;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;

public class JsonPathSubscriptionMessageFilterCompiler implements SubscriptionMessageFilterCompiler {
    private Configuration configuration = defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS);

    @Override
    public String getType() {
        return "jsonpath";
    }

    @Override
    public Predicate<Message> compile(MessageFilterSpecification specification) {
        return new JsonPathPredicate(specification.getPath(), Pattern.compile(specification.getMatcher()), configuration);
    }
}
