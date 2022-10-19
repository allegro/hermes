package pl.allegro.tech.hermes.management.api.writer;

import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;
import pl.allegro.tech.hermes.api.UnhealthySubscription;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import static java.util.stream.Collectors.joining;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

@Provider
@Produces(TEXT_PLAIN)
public class UnhealthySubscriptionListPlainTextBodyWriter implements MessageBodyWriter<List<UnhealthySubscription>> {

    private static String toPlainText(UnhealthySubscription unhealthySubscription) {
        String problemDescriptions = unhealthySubscription.getProblems().stream()
                .map(SubscriptionHealthProblem::getDescription)
                .collect(joining("; "));
        return unhealthySubscription.getName() + " - " + problemDescriptions;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (List.class.isAssignableFrom(type) && genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type[] actualTypeArgs = parameterizedType.getActualTypeArguments();
            return actualTypeArgs.length == 1 && actualTypeArgs[0].equals(UnhealthySubscription.class);
        }
        return false;
    }

    @Override
    public long getSize(List<UnhealthySubscription> unhealthySubscriptionList,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType) {
        return -1; // According to JAX-RS 2.0 spec this method is deprecated and should return -1
    }

    @Override
    public void writeTo(List<UnhealthySubscription> unhealthySubscriptionList,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        String body = unhealthySubscriptionList.stream()
                .map(UnhealthySubscriptionListPlainTextBodyWriter::toPlainText)
                .collect(joining("\r\n"));
        entityStream.write(body.getBytes());
    }
}
