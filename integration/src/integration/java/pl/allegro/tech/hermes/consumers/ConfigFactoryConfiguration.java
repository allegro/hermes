package pl.allegro.tech.hermes.consumers;

import org.apache.commons.beanutils.ConvertUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
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
    @Profile("integration")
    public ConfigFactory testConfigFactory(ApplicationArguments applicationArguments) {
        MutableConfigFactory configFactory = new MutableConfigFactory();
        List<String> values = Arrays.stream(Configs.values()).map(Configs::getName).collect(Collectors.toList());

        applicationArguments.getOptionNames().stream()
                .filter(values::contains)
                .collect(Collectors.toMap(Configs::getForName, option -> getValue(applicationArguments.getOptionValues(option), option)))
                .forEach(configFactory::overrideProperty);

        return configFactory;
    }

    private Object getValue(List<String> list, String name) {
        Class<?> clazz = Configs.getForName(name).getDefaultValue().getClass();
        if (list.size() > 1) {
            return list.stream().collect(Collectors.joining(",", "", ""));
        }
        return ConvertUtils.convert(list.get(0), clazz);
    }
}
