package pl.allegro.tech.hermes.management.api.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.Query;
import pl.allegro.tech.hermes.management.infrastructure.query.parser.QueryParser;
import pl.allegro.tech.hermes.management.infrastructure.query.parser.json.JsonQueryParser;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

@Provider
public class QueryBodyReader implements MessageBodyReader<Query> {

    private final ObjectMapper objectMapper;

    @Autowired
    public QueryBodyReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Query.class;
    }

    @Override
    public Query readFrom(Class<Query> type,
                          Type genericType,
                          Annotation[] annotations,
                          MediaType mediaType,
                          MultivaluedMap<String, String> httpHeaders,
                          InputStream entityStream) throws IOException, WebApplicationException {

        Class<?> queryType = Object.class;
        if (genericType instanceof ParameterizedType) {
            queryType = (Class<?>) ((ParameterizedType) genericType).getRawType();
        }
        QueryParser parser = new JsonQueryParser(objectMapper);
        return parser.parse(entityStream, queryType);
    }
}
