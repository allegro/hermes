package pl.allegro.tech.hermes.common.filtering.json;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.common.filtering.FilteringException;
import pl.allegro.tech.hermes.common.message.MessageContent;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static pl.allegro.tech.hermes.common.filtering.FilteringException.check;

public class JsonPathPredicate implements Predicate<MessageContent> {
    private Configuration configuration;
    private String path;
    private Pattern matcher;

    public JsonPathPredicate(String path, Pattern matcher, Configuration configuration) {
        this.path = path;
        this.matcher = matcher;
        this.configuration = configuration;
    }

    @Override
    public boolean test(MessageContent message) {
        check(message.getContentType() == ContentType.JSON, "This filter supports only JSON contentType.");
        try {
            List<Object> result = JsonPath.parse(new ByteArrayInputStream(message.getData()), configuration).read(path);
            return !result.isEmpty() && result.stream()
                    .map(Objects::toString)
                    .allMatch(o -> matcher.matcher(o).matches());
        } catch (InvalidJsonException ex) {
            throw new FilteringException("Corrupted message - could not parse", ex);
        } catch (Exception ex) {
            throw new FilteringException(ex);
        }
    }
}
