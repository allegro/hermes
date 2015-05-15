package pl.allegro.tech.hermes.consumers.consumer.sender;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public abstract class MessageSenderProviderFactory<T extends ProtocolMessageSenderProvider> implements Factory<T> {

    protected final ConfigFactory configFactory;

    public MessageSenderProviderFactory(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }

    @Override
    public T provide() {
        T instance = provideInstance();

        try {
            instance.start();
            return instance;
        } catch (Exception ex) {
            throw new InternalProcessingException(ex);
        }
    }

    protected abstract T provideInstance();

    @Override
    public void dispose(T instance) {
        try {
            instance.stop();
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }
}
