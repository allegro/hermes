package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.common.base.Preconditions;
import com.google.pubsub.v1.TopicName;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import pl.allegro.tech.hermes.api.EndpointAddress;

public class GooglePubSubSenderTargetResolver {

  GooglePubSubSenderTarget resolve(EndpointAddress address) {
    try {
      final URI uri = URI.create(address.getRawEndpoint());
      Preconditions.checkArgument(uri.getScheme().equals("googlepubsub"));
      Preconditions.checkArgument(uri.getPort() > 0);

      return GooglePubSubSenderTarget.builder()
          .withPubSubEndpoint(uri.getAuthority())
          .withTopicName(TopicName.parse(uri.getPath().substring(1)))
          .withCompressionCodec(findCompressionCodec(uri.getQuery()))
          .build();
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Given endpoint is invalid", e);
    }
  }

  private CompressionCodec findCompressionCodec(String params) {
    try {
      Map<String, List<String[]>> paramListMap =
          Optional.ofNullable(params)
              .map(
                  q ->
                      Arrays.stream(q.split("&"))
                          .map(p -> p.split("="))
                          .filter(p -> p.length > 1)
                          .collect(Collectors.groupingBy(c -> c[0])))
              .orElse(Collections.emptyMap());

      return Optional.ofNullable(paramListMap.get("compression"))
          .flatMap(p -> p.stream().findFirst())
          .flatMap(p -> Optional.ofNullable(p[1]))
          .map(String::toUpperCase)
          .map(CompressionCodec::valueOf)
          .orElse(CompressionCodec.EMPTY);
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException("Unsupported compression codec", ex);
    }
  }
}
