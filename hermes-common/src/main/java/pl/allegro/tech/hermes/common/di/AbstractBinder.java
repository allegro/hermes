package pl.allegro.tech.hermes.common.di;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.ScopedBindingBuilder;

import javax.inject.Singleton;
import java.lang.reflect.ParameterizedType;

public abstract class AbstractBinder extends org.glassfish.hk2.utilities.binding.AbstractBinder {

    protected <T> ScopedBindingBuilder<T> bindSingleton(Class<T> clazz) {
        return bind(clazz).in(Singleton.class).to(clazz);
    }

    protected <T> ScopedBindingBuilder<T> bindSingletonFactory(Class<? extends Factory<T>> genericClass) {
        return bindFactory(genericClass).in(Singleton.class).to(factoryOfType(genericClass));
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> factoryOfType(Class<? extends Factory<T>> clazz) {
        return (Class<T>) ((ParameterizedType) clazz.getGenericInterfaces()[0]).getActualTypeArguments()[0];
    }
}
