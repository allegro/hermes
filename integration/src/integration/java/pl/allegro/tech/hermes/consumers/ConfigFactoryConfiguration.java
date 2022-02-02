package pl.allegro.tech.hermes.consumers;

import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class ConfigFactoryConfiguration {

    @Bean
    @Primary
    public ConfigFactory testConfigFactory(ApplicationArguments applicationArguments) {
        MutableConfigFactory configFactory = new MutableConfigFactory();
        List<String> values = Arrays.stream(Configs.values()).map(Configs::getName).collect(Collectors.toList());

        applicationArguments.getOptionNames().stream()
                .filter(values::contains)
                .collect(Collectors.toMap(Configs::getForName, option -> getValue(applicationArguments.getOptionValues(option), option)))
                .forEach(configFactory::overrideProperty);

        return configFactory;
    }

    private Object getValue(List<String> list, String name) { //TODO: refactor
        Class<?> clazz = Configs.getForName(name).getDefaultValue().getClass();
        if (list.size() > 1) {
            return list.stream().collect(Collectors.joining(",", "", ""));
        }
        if (Boolean.class.equals(clazz)) {
            return Boolean.valueOf(list.get(0));
        } else if (Integer.class.equals(clazz)) {
            return Integer.valueOf(list.get(0));
        } else if (Double.class.equals(clazz)) {
            return Double.valueOf(list.get(0));
        } else if (Long.class.equals(clazz)) {
            return Long.valueOf(list.get(0));
        } else if (Float.class.equals(clazz)) {
            return Float.valueOf(list.get(0));
        }
        return list.get(0);
    }
}
