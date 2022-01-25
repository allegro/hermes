package pl.allegro.tech.hermes.consumers.di.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;

public class PrimaryBeanCustomizer implements BeanDefinitionCustomizer {

    @Override
    public void customize(BeanDefinition bd) {
        bd.setPrimary(true);
    }
}
