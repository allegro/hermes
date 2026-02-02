package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.domain.group.GroupRepository;
import pl.allegro.tech.hermes.management.api.auth.ManagementRights;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.domain.group.GroupValidator;
import pl.allegro.tech.hermes.management.domain.owner.validator.OwnerIdValidator;
import pl.allegro.tech.hermes.management.domain.topic.validator.ContentTypeValidator;
import pl.allegro.tech.hermes.management.domain.topic.validator.TopicValidator;
import pl.allegro.tech.hermes.schema.SchemaRepository;

@Configuration
@EnableConfigurationProperties({GroupProperties.class, TopicProperties.class})
public class ValidatorConfiguration {

  @Bean
  public ApiPreconditions apiPreconditions() {
    return new ApiPreconditions();
  }

  @Bean
  public ManagementRights managementRights(GroupProperties groupProperties) {
    return new ManagementRights(groupProperties);
  }

  @Bean
  public GroupValidator groupValidator(
      GroupRepository groupRepository, GroupProperties groupProperties) {
    return new GroupValidator(groupRepository, groupProperties.getAllowedGroupNameRegex());
  }

  @Bean
  public TopicValidator topicValidator(
      OwnerIdValidator ownerIdValidator,
      ContentTypeValidator contentTypeValidator,
      SchemaRepository schemaRepository,
      ApiPreconditions apiPreconditions,
      TopicProperties topicProperties) {
    return new TopicValidator(
        ownerIdValidator,
        contentTypeValidator,
        schemaRepository,
        apiPreconditions,
        topicProperties.isDefaultFallbackToRemoteDatacenterEnabled());
  }

  @Bean
  public ContentTypeValidator contentTypeValidator(TopicProperties topicProperties) {
    return new ContentTypeValidator(
        new java.util.HashSet<>(topicProperties.getAllowedContentTypes()));
  }
}
