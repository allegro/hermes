package pl.allegro.tech.hermes.consumers.consumer.sender;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.mockito.Mockito;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.exception.EndpointProtocolNotSupportedException;
import pl.allegro.tech.hermes.consumers.consumer.ResilientMessageSender;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

public class MessageSenderFactoryTest {

    private final MessageSender referenceMessageSender = Mockito.mock(MessageSender.class);
    private final ResilientMessageSender resilientMessageSender = Mockito.mock(ResilientMessageSender.class);

    @Test
    public void shouldCreateCustomProtocolMessageSender() {
        // given
        EndpointAddress endpoint = EndpointAddress.of("myProtocol://service");
        Subscription subscription = subscription("group.topic", "subscription", endpoint).build();
        MessageSenderFactory factory = new MessageSenderFactory(ImmutableList.of(
                protocolMessageSenderProviderReturning(referenceMessageSender, "myProtocol"))
        );

        // when
        MessageSender sender = factory.create(subscription, resilientMessageSender);

        // then
        assertThat(sender).isEqualTo(referenceMessageSender);
    }

    @Test
    public void shouldGetProtocolNotSupportedExceptionWhenPassingUnknownUri() {
        // given
        MessageSenderFactory factory = new MessageSenderFactory(ImmutableList.of());
        Subscription subscription = subscription("group.topic", "subscription", "unknown://localhost:8080/test").build();

        // then
        assertThrows(EndpointProtocolNotSupportedException.class, () -> factory.create(subscription, resilientMessageSender));
    }

    private ProtocolMessageSenderProvider protocolMessageSenderProviderReturning(Object createdMessageSender, String protocol) {
        return new ProtocolMessageSenderProvider() {
            @Override
            public MessageSender create(Subscription endpoint, ResilientMessageSender resilientMessageSender) {
                return (MessageSender) createdMessageSender;
            }

            @Override
            public Set<String> getSupportedProtocols() {
                return ImmutableSet.of(protocol);
            }

            @Override
            public void start() throws Exception {
            }

            @Override
            public void stop() throws Exception {
            }
        };
    }
}
