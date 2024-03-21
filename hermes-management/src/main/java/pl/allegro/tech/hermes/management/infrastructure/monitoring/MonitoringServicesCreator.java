package pl.allegro.tech.hermes.management.infrastructure.monitoring;

import org.apache.kafka.clients.admin.AdminClient;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.management.config.kafka.AdminClientFactory;
import pl.allegro.tech.hermes.management.config.kafka.KafkaClustersProperties;
import pl.allegro.tech.hermes.management.config.kafka.KafkaNamesMappers;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class MonitoringServicesCreator {

    private final KafkaClustersProperties kafkaClustersProperties;
    private final KafkaNamesMappers kafkaNamesMappers;

    public MonitoringServicesCreator(KafkaClustersProperties kafkaClustersProperties, KafkaNamesMappers kafkaNamesMappers) {
        this.kafkaClustersProperties = kafkaClustersProperties;
        this.kafkaNamesMappers = kafkaNamesMappers;
    }

    public List<MonitoringService> createMonitoringServices() {
        return kafkaClustersProperties.getClusters().stream().map(kafkaProperties -> {
            KafkaNamesMapper kafkaNamesMapper = kafkaNamesMappers.getMapper(kafkaProperties.getQualifiedClusterName());
            AdminClient brokerAdminClient = AdminClientFactory.brokerAdminClient(kafkaProperties);

            return new MonitoringService(
                    kafkaNamesMapper,
                    brokerAdminClient,
                    kafkaProperties.getQualifiedClusterName());
        }).collect(toList());
    }
}
