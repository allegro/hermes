package pl.allegro.tech.hermes.integration.env;

import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.server.spring.SpringComponentProvider;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

final class SpringlessServiceIteratorProvider extends ServiceFinder.ServiceIteratorProvider {

    static final SpringlessServiceIteratorProvider INSTANCE = new SpringlessServiceIteratorProvider();

    private static final ServiceFinder.ServiceIteratorProvider DELEGATE = new ServiceFinder.DefaultServiceIteratorProvider();

    private SpringlessServiceIteratorProvider() {
    }

    @Override
    public <T> Iterator<T> createIterator(Class<T> service,
                                          String serviceName,
                                          ClassLoader loader,
                                          boolean ignoreOnClassNotFound) {
        Iterator<T> iterator = DELEGATE.createIterator(service, serviceName, loader, ignoreOnClassNotFound);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .filter(o -> !o.getClass().getCanonicalName().equals(SpringComponentProvider.class.getCanonicalName()))
                .iterator();
    }

    @Override
    public <T> Iterator<Class<T>> createClassIterator(Class<T> service,
                                                      String serviceName,
                                                      ClassLoader loader,
                                                      boolean ignoreOnClassNotFound) {
        return DELEGATE.createClassIterator(service, serviceName, loader, ignoreOnClassNotFound);
    }
}
