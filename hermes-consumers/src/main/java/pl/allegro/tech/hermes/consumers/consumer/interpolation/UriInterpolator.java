package pl.allegro.tech.hermes.consumers.consumer.interpolation;

import java.net.URI;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.Message;

public interface UriInterpolator {
  URI interpolate(EndpointAddress endpoint, Message message) throws InterpolationException;
}
