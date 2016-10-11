package pl.allegro.tech.hermes.management.api.mappers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.common.filtering.MessageFilter;
import pl.allegro.tech.hermes.common.filtering.json.JsonPathPredicate;
import pl.allegro.tech.hermes.common.message.MessageContent;

import java.util.List;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

public class MessageValidationWrapper {
    private final MessageContent message;
    private final List<MessageFilter> messageFilterList;

    @Override
    public String toString() {
        return "MessageValidationWrapper{" +
                "message=" + message +
                ", messageFilterList=" + messageFilterList +
                '}';
    }

    public MessageContent getMessage() {
        return message;
    }

    @JsonCreator
    public MessageValidationWrapper(@JsonProperty("message") String stringMessage,
                                    @JsonProperty("filters") List<MessageFilterSpecification> filters) {
        this.messageFilterList = filters.stream()
                .map(f -> new JsonPathPredicate(
                        f.getPath(),
                        Pattern.compile(f.getMatcher()),
                        Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS)))
                .map(f -> new MessageFilter("jsonpath", f))
                .collect(toList());
        this.message = new MessageContent.Builder()
                .withData(stringMessage.getBytes())
                .withContentType(ContentType.JSON)
                .build();
    }

    public List<MessageFilter> getMessageFilterList() {
        return messageFilterList;
    }
}