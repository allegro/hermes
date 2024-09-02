package pl.allegro.tech.hermes.management.api.writer;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static java.util.stream.Collectors.joining;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;
import pl.allegro.tech.hermes.api.UnhealthySubscription;

@Provider
@Produces(TEXT_PLAIN)
public class UnhealthySubscriptionListPlainTextBodyWriter
    implements MessageBodyWriter<List<UnhealthySubscription>> {

  private static String toPlainText(UnhealthySubscription unhealthySubscription) {
    String problemDescriptions =
        unhealthySubscription.getProblems().stream()
            .map(SubscriptionHealthProblem::getDescription)
            .collect(joining("; "));
    return unhealthySubscription.getName() + " - " + problemDescriptions;
  }

  @Override
  public boolean isWriteable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (List.class.isAssignableFrom(type) && genericType instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) genericType;
      Type[] actualTypeArgs = parameterizedType.getActualTypeArguments();
      return actualTypeArgs.length == 1 && actualTypeArgs[0].equals(UnhealthySubscription.class);
    }
    return false;
  }

  @Override
  public long getSize(
      List<UnhealthySubscription> unhealthySubscriptionList,
      Class<?> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType) {
    return -1; // According to JAX-RS 2.0 spec this method is deprecated and should return -1
  }

  @Override
  public void writeTo(
      List<UnhealthySubscription> unhealthySubscriptionList,
      Class<?> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, Object> httpHeaders,
      OutputStream entityStream)
      throws IOException, WebApplicationException {
    String body =
        unhealthySubscriptionList.stream()
            .map(UnhealthySubscriptionListPlainTextBodyWriter::toPlainText)
            .collect(joining("\r\n"));
    entityStream.write(body.getBytes());
  }
}
