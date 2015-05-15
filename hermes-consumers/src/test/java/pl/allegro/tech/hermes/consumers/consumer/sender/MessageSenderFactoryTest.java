package pl.allegro.tech.hermes.consumers.consumer.sender;

import com.googlecode.catchexception.CatchException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.exception.EndpointProtocolNotSupportedException;

import static com.googlecode.catchexception.CatchException.catchException;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.Subscription.Builder.subscription;

@RunWith(MockitoJUnitRunner.class)
public class MessageSenderFactoryTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ProtocolMessageSenderProvider messageSenderProvider;

    @Mock
    private ProtocolMessageSenderProvider defaultMessageSenderProvider;

    @Test
    public void shouldCreateCustomProtocolMessageSender() {
        // given
        final String endpoint = "myProtocol://service";
        Subscription subscription = subscription().withEndpoint(EndpointAddress.of(endpoint)).build();

        MessageSenderFactory messageSenderFactory = new MessageSenderFactory(
            providersFrom("myProtocol", messageSenderProvider), defaultMessageSenderProvider, defaultMessageSenderProvider
        );

        // when & then
        assertThat(messageSenderFactory.create(subscription)).isEqualTo(messageSenderProvider.create(endpoint));
    }

    @Test
    public void shouldCreateHttpMessageSenderWhenPassingHttpUri() {
        // given
        final String endpoint = "http://192.168.0.1";
        MessageSenderFactory messageSenderFactory = new MessageSenderFactory(
            new MessageSenderProviders(), messageSenderProvider, defaultMessageSenderProvider
        );
        Subscription subscription = subscription().withEndpoint(EndpointAddress.of(endpoint)).build();

        // when & then
        assertThat(messageSenderFactory.create(subscription)).isEqualTo(messageSenderProvider.create(endpoint));
    }

    @Test
    public void shouldCreateJmsMessageSenderWhenPassingHttpUri() {
        // given
        final String endpoint = "jms://192.168.0.1/topic";
        MessageSenderFactory messageSenderFactory = new MessageSenderFactory(
                new MessageSenderProviders(), defaultMessageSenderProvider, messageSenderProvider
        );
        Subscription subscription = subscription().withEndpoint(EndpointAddress.of(endpoint)).build();

        // when & then
        assertThat(messageSenderFactory.create(subscription)).isEqualTo(messageSenderProvider.create(endpoint));
    }

    @Test
    public void shouldGetProtocolNotSupportedExceptionWhenPassingUnknownUri() {
        // given
        MessageSenderFactory messageSenderFactory = new MessageSenderFactory(
            new MessageSenderProviders(), defaultMessageSenderProvider, defaultMessageSenderProvider
        );
        Subscription subscription = subscription().withEndpoint(EndpointAddress.of("unknown://localhost:8080/test")).build();

        // when
        catchException(messageSenderFactory).create(subscription);

        // then
        assertThat(CatchException.<EndpointProtocolNotSupportedException>caughtException())
            .isInstanceOf(EndpointProtocolNotSupportedException.class);
    }

    @Test
    public void shouldOverrideDefaultProtocolMessageSender() {
        // given
        final String endpoint = "http://service";
        Subscription subscription = subscription().withEndpoint(EndpointAddress.of(endpoint)).build();
        MessageSenderFactory messageSenderFactory = new MessageSenderFactory(
            providersFrom("http", messageSenderProvider), defaultMessageSenderProvider, defaultMessageSenderProvider
        );

        // when & then
        assertThat(messageSenderFactory.create(subscription)).isEqualTo(messageSenderProvider.create(endpoint));
    }

    private MessageSenderProviders providersFrom(String protocol, ProtocolMessageSenderProvider protocolMessageSenderProvider) {
        MessageSenderProviders providers = new MessageSenderProviders();
        providers.put(protocol, protocolMessageSenderProvider);

        return providers;
    }
}
