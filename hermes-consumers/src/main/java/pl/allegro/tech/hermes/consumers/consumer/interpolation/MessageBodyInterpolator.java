package pl.allegro.tech.hermes.consumers.consumer.interpolation;

import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.VariableExpansionException;
import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.util.Map;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.Message;

public class MessageBodyInterpolator implements UriInterpolator {

  private static final int MAX_CACHE_SIZE = 1000;

  private final LoadingCache<String, UriTemplate> templateCache =
      CacheBuilder.newBuilder().maximumSize(MAX_CACHE_SIZE).build(new TemplateLoader());

  private final LoadingCache<String, JsonPath> variableCompiler =
      CacheBuilder.newBuilder().maximumSize(MAX_CACHE_SIZE).build(new JsonPathLoader());

  public URI interpolate(EndpointAddress endpoint, Message message) throws InterpolationException {
    UriTemplate template = templateCache.getUnchecked(endpoint.getEndpoint());
    String[] variables = template.getVariables();

    if (variables.length > 0) {
      Map<String, Object> values = Maps.newHashMap();
      String payload = new String(message.getData(), Charsets.UTF_8);

      for (String variable : variables) {

        JsonPath path = variableCompiler.getUnchecked(variable);

        try {
          values.put(variable, path.read(payload));
        } catch (InvalidPathException e) {
          throw new InterpolationException(
              String.format("Missing variable on path %s", path.getPath()), e);
        }
      }

      try {
        return URI.create(template.expand(values));
      } catch (VariableExpansionException e) {
        throw new InterpolationException("Cannot expand template", e);
      }
    }

    return endpoint.getUri();
  }

  private static class TemplateLoader extends CacheLoader<String, UriTemplate> {

    @Override
    public UriTemplate load(String url) throws Exception {
      return UriTemplate.fromTemplate(url);
    }
  }

  private static class JsonPathLoader extends CacheLoader<String, JsonPath> {

    private static final String ROOT_PREFIX = "$.";

    @Override
    public JsonPath load(String key) throws Exception {
      return JsonPath.compile(ROOT_PREFIX + key);
    }
  }
}
