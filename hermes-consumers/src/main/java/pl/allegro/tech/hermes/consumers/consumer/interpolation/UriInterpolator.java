package pl.allegro.tech.hermes.consumers.consumer.interpolation;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;

import java.net.URI;

public interface UriInterpolator {
    URI interpolate(EndpointAddress endpoint, Message message) throws InterpolationException;
}
