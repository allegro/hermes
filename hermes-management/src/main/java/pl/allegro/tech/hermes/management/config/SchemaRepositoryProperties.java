package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.config.Configs;

@ConfigurationProperties(prefix = "schemaRepository")
public class SchemaRepositoryProperties {

    private String repositoryType = Configs.SCHEMA_REPOSITORY_TYPE.getDefaultValue();

    private String schemaRepoServerUrl = Configs.SCHEMA_REPO_SERVER_URL.getDefaultValue();

    public String getRepositoryType() {
        return repositoryType;
    }

    public void setRepositoryType(String repositoryType) {
        this.repositoryType = repositoryType;
    }

    public String getSchemaRepoServerUrl() {
        return schemaRepoServerUrl;
    }

    public void setSchemaRepoServerUrl(String schemaRepoServerUrl) {
        this.schemaRepoServerUrl = schemaRepoServerUrl;
    }
}
