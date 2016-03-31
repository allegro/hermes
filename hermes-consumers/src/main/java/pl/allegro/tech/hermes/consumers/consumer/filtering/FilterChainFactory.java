package pl.allegro.tech.hermes.consumers.consumer.filtering;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import pl.allegro.tech.hermes.api.Subscription;
import wandou.avpath.Parser;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class FilterChainFactory {
    ObjectMapper mapper = new ObjectMapper();

    public FilterChain create(final Subscription subscription) {
        try {
            final List<String> filters = subscription.getFilters();
            Map filter = (Map)mapper.readValue(filters.get(0), Map.class).get("expression");
            Parser.PathSyntax ast = new Parser().parse(filter.get("path").toString());
            Pattern pattern = Pattern.compile(filter.get("matcher").toString());
            return new FilterChain(ImmutableList.of(new AvroPathMessageFilter(ast, pattern)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
