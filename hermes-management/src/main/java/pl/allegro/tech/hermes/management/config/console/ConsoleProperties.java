package pl.allegro.tech.hermes.management.config.console;

import com.google.common.collect.Lists;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This class could have a much better structure, however it has this one due to compatibility with old JSON config file format.
 */
@ConfigurationProperties(prefix = "console")
public class ConsoleProperties {
    private Console console = new Console();
    private Dashboard dashboard = new Dashboard();
    private Hermes hermes = new Hermes();
    private Metrics metrics = new Metrics();
    private Auth auth = new Auth();
    private Owner owner = new Owner();
    private TopicView topic = new TopicView();
    private SubscriptionView subscription = new SubscriptionView();
    private ConsistencyView consistency = new ConsistencyView();
    private GroupView group = new GroupView();

    public Dashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public Hermes getHermes() {
        return hermes;
    }

    public void setHermes(Hermes hermes) {
        this.hermes = hermes;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Console getConsole() {
        return console;
    }

    public void setConsole(Console console) {
        this.console = console;
    }

    public ConsistencyView getConsistency() {
        return consistency;
    }

    public void setConsistency(ConsistencyView consistency) {
        this.consistency = consistency;
    }

    public GroupView getGroup() {
        return group;
    }

    public void setGroup(GroupView group) {
        this.group = group;
    }

    public static final class Console {
        private String title = "hermes console";
        private String footer = "";


        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getFooter() {
            return footer;
        }

        public void setFooter(String footer) {
            this.footer = footer;
        }
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public TopicView getTopic() {
        return topic;
    }

    public void setTopic(TopicView topic) {
        this.topic = topic;
    }

    public SubscriptionView getSubscription() {
        return subscription;
    }

    public void setSubscription(SubscriptionView subscription) {
        this.subscription = subscription;
    }

    public static final class Dashboard {
        private String metrics = "http://localhost:8082";
        private String docs = "http://hermes-pubsub.rtfd.org";

        public String getMetrics() {
            return metrics;
        }

        public void setMetrics(String metrics) {
            this.metrics = metrics;
        }

        public String getDocs() {
            return docs;
        }

        public void setDocs(String docs) {
            this.docs = docs;
        }
    }

    public static final class Hermes {
        private Discovery discovery = new Discovery();

        public Discovery getDiscovery() {
            return discovery;
        }

        public void setDiscovery(Discovery discovery) {
            this.discovery = discovery;
        }
    }

    public static final class Discovery {
        private String type = "simple";
        private SimpleDiscovery simple = new SimpleDiscovery();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public SimpleDiscovery getSimple() {
            return simple;
        }

        public void setSimple(SimpleDiscovery simple) {
            this.simple = simple;
        }
    }

    public static final class SimpleDiscovery {
        private String url = "";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static final class Metrics {
        private String type = "graphite";
        private Graphite graphite = new Graphite();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Graphite getGraphite() {
            return graphite;
        }

        public void setGraphite(Graphite graphite) {
            this.graphite = graphite;
        }
    }

    public static final class Graphite {
        private String url = "localhost:8082";
        private String prefix = "hermes";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }

    public static final class Auth {
        private OAuth oauth = new OAuth();
        private AuthHeaders headers = new AuthHeaders();

        public OAuth getOauth() {
            return oauth;
        }

        public void setOauth(OAuth oauth) {
            this.oauth = oauth;
        }

        public AuthHeaders getHeaders() {
            return headers;
        }

        public void setHeaders(AuthHeaders headers) {
            this.headers = headers;
        }
    }

    public static final class OAuth {
        private boolean enabled = false;
        private String url = "localhost:8092/auth";
        private String clientId = "hermes";
        private String scope = "hermes";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }

    public static final class AuthHeaders {
        private boolean enabled = false;
        private String adminHeader = "Hermes-Admin-Password";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAdminHeader() {
            return adminHeader;
        }

        public void setAdminHeader(String adminHeader) {
            this.adminHeader = adminHeader;
        }
    }

    public static final class TopicView {
        private boolean messagePreviewEnabled = true;
        private boolean offlineClientsEnabled = false;
        private boolean authEnabled = true;
        private DefaultTopicView defaults = new DefaultTopicView();
        private String buttonsExtension = "";
        private boolean removeSchema = false;
        private boolean schemaIdAwareSerializationEnabled = false;
        private boolean avroContentTypeMetadataRequired = true;
        private List<TopicContentType> contentTypes = Lists.newArrayList(
                new TopicContentType("AVRO", "AVRO"),
                new TopicContentType("JSON", "JSON")
        );
        private boolean readOnlyModeEnabled = false;
        private Set<String> allowedTopicLabels = Collections.emptySet();
        private List<RetentionUnit> retentionUnits = Lists.newArrayList(
                new RetentionUnit("DAYS", "DAYS")
        );
        private boolean offlineRetransmissionEnabled = false;
        private String offlineRetransmissionDescription = "Offline retransmission";

        public boolean isMessagePreviewEnabled() {
            return messagePreviewEnabled;
        }

        public void setMessagePreviewEnabled(boolean messagePreviewEnabled) {
            this.messagePreviewEnabled = messagePreviewEnabled;
        }

        public boolean isOfflineClientsEnabled() {
            return offlineClientsEnabled;
        }

        public void setOfflineClientsEnabled(boolean offlineClientsEnabled) {
            this.offlineClientsEnabled = offlineClientsEnabled;
        }

        public boolean isAuthEnabled() {
            return authEnabled;
        }

        public void setAuthEnabled(boolean authEnabled) {
            this.authEnabled = authEnabled;
        }

        public DefaultTopicView getDefaults() {
            return defaults;
        }

        public void setDefaults(DefaultTopicView defaults) {
            this.defaults = defaults;
        }

        public List<TopicContentType> getContentTypes() {
            return contentTypes;
        }

        public void setContentTypes(List<TopicContentType> contentTypes) {
            this.contentTypes = contentTypes;
        }

        public Set<String> getAllowedTopicLabels() {
            return allowedTopicLabels;
        }

        public void setAllowedTopicLabels(Set<String> allowedTopicLabels) {
            this.allowedTopicLabels = allowedTopicLabels;
        }

        public String getButtonsExtension() {
            return buttonsExtension;
        }

        public void setButtonsExtension(String buttonsExtension) {
            this.buttonsExtension = buttonsExtension;
        }

        public boolean isRemoveSchema() {
            return removeSchema;
        }

        public void setRemoveSchema(boolean removeSchema) {
            this.removeSchema = removeSchema;
        }

        public boolean isSchemaIdAwareSerializationEnabled() {
            return schemaIdAwareSerializationEnabled;
        }

        public void setSchemaIdAwareSerializationEnabled(boolean schemaIdAwareSerializationEnabled) {
            this.schemaIdAwareSerializationEnabled = schemaIdAwareSerializationEnabled;
        }

        public boolean isAvroContentTypeMetadataRequired() {
            return avroContentTypeMetadataRequired;
        }

        public void setAvroContentTypeMetadataRequired(boolean avroContentTypeMetadataRequired) {
            this.avroContentTypeMetadataRequired = avroContentTypeMetadataRequired;
        }

        public boolean isReadOnlyModeEnabled() {
            return readOnlyModeEnabled;
        }

        public void setReadOnlyModeEnabled(boolean readOnlyModeEnabled) {
            this.readOnlyModeEnabled = readOnlyModeEnabled;
        }

        public List<RetentionUnit> getRetentionUnits() {
            return retentionUnits;
        }

        public void setRetentionUnits(List<RetentionUnit> retentionUnits) {
            this.retentionUnits = retentionUnits;
        }

        public boolean getOfflineRetransmissionEnabled() {
            return offlineRetransmissionEnabled;
        }

        public void setOfflineRetransmissionEnabled(boolean offlineRetransmissionEnabled) {
            this.offlineRetransmissionEnabled = offlineRetransmissionEnabled;
        }

        public String getOfflineRetransmissionDescription() {
            return offlineRetransmissionDescription;
        }

        public void setOfflineRetransmissionDescription(String offlineRetransmissionDescription) {
            this.offlineRetransmissionDescription = offlineRetransmissionDescription;
        }
    }

    public static final class DefaultTopicView {
        private String ack = "LEADER";
        private String contentType = "JSON";
        private RetentionTime retentionTime = new RetentionTime();
        private DefaultOfflineStorageView offlineStorage = new DefaultOfflineStorageView();

        public String getAck() {
            return ack;
        }

        public void setAck(String ack) {
            this.ack = ack;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public RetentionTime getRetentionTime() {
            return retentionTime;
        }

        public void setRetentionTime(RetentionTime retentionTime) {
            this.retentionTime = retentionTime;
        }

        public DefaultOfflineStorageView getOfflineStorage() {
            return offlineStorage;
        }

        public void setOfflineStorage(DefaultOfflineStorageView offlineStorage) {
            this.offlineStorage = offlineStorage;
        }
    }

    public static final class DefaultOfflineStorageView {
        private boolean enabled = false;
        private RetentionTime retentionTime = new RetentionTime();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public RetentionTime getRetentionTime() {
            return retentionTime;
        }

        public void setRetentionTime(RetentionTime retentionTime) {
            this.retentionTime = retentionTime;
        }
    }

    public static final class RetentionTime {
        private int duration = 1;
        private TimeUnit retentionUnit = TimeUnit.DAYS;

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        public TimeUnit getRetentionUnit() {
            return retentionUnit;
        }

        public void setRetentionUnit(TimeUnit retentionUnit) {
            this.retentionUnit = retentionUnit;
        }
    }

    public static final class TopicContentType {
        private String value = "";
        private String label = "";

        public TopicContentType() {
        }

        public TopicContentType(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static final class RetentionUnit {
        private String value = "";
        private String label = "";

        public RetentionUnit() {
        }

        public RetentionUnit(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static final class Owner {
        private List<OwnerSource> sources = Lists.newArrayList(
                new OwnerSource("Crowd", "Crowd group (or groups separated by ',')"));

        public List<OwnerSource> getSources() {
            return sources;
        }

        public void setSources(List<OwnerSource> sources) {
            this.sources = sources;
        }
    }

    public static final class OwnerSource {
        private String name = "";
        private String placeholder = "";

        public OwnerSource() {
        }

        public OwnerSource(String name, String placeholder) {
            this.name = name;
            this.placeholder = placeholder;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPlaceholder() {
            return placeholder;
        }

        public void setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
        }
    }

    public static final class SubscriptionView {
        private Map<String, EndpointAddressResolverMetadata> endpointAddressResolverMetadata = new HashMap<>();
        private boolean showHeadersFilter = false;
        private boolean showFixedHeaders = false;
        private int requestTimeoutWarningThreshold = 1001;
        private DefaultSubscriptionView defaults = new DefaultSubscriptionView();
        private List<SubscriptionDeliveryType> deliveryTypes = Lists.newArrayList(
                new SubscriptionDeliveryType("SERIAL", "SERIAL"),
                new SubscriptionDeliveryType("BATCH", "BATCH")
        );

        public Map<String, EndpointAddressResolverMetadata> getEndpointAddressResolverMetadata() {
            return endpointAddressResolverMetadata;
        }

        public void setEndpointAddressResolverMetadata(Map<String, EndpointAddressResolverMetadata> endpointAddressResolverMetadata) {
            this.endpointAddressResolverMetadata = endpointAddressResolverMetadata;
        }

        public boolean isShowHeadersFilter() {
            return showHeadersFilter;
        }

        public void setShowHeadersFilter(boolean showHeadersFilter) {
            this.showHeadersFilter = showHeadersFilter;
        }

        public DefaultSubscriptionView getDefaults() {
            return defaults;
        }

        public void setDefaults(DefaultSubscriptionView defaults) {
            this.defaults = defaults;
        }

        public List<SubscriptionDeliveryType> getDeliveryTypes() {
            return deliveryTypes;
        }

        public void setDeliveryTypes(List<SubscriptionDeliveryType> deliveryTypes) {
            this.deliveryTypes = deliveryTypes;
        }

        public boolean isShowFixedHeaders() {
            return showFixedHeaders;
        }

        public void setShowFixedHeaders(boolean showFixedHeaders) {
            this.showFixedHeaders = showFixedHeaders;
        }

        public int getRequestTimeoutWarningThreshold() {
            return requestTimeoutWarningThreshold;
        }

        public void setRequestTimeoutWarningThreshold(int requestTimeoutWarningThreshold) {
            this.requestTimeoutWarningThreshold = requestTimeoutWarningThreshold;
        }
    }

    public static final class GroupView {
        private boolean nonAdminCreationEnabled = false;

        public boolean isNonAdminCreationEnabled() {
            return nonAdminCreationEnabled;
        }

        public void setNonAdminCreationEnabled(boolean nonAdminCreationEnabled) {
            this.nonAdminCreationEnabled = nonAdminCreationEnabled;
        }
    }

    public static final class EndpointAddressResolverMetadata {
        private String title;
        private String type;
        private String hint;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getHint() {
            return hint;
        }

        public void setHint(String hint) {
            this.hint = hint;
        }
    }

    public static final class DefaultSubscriptionView {
        private SubscriptionPolicy subscriptionPolicy = new SubscriptionPolicy();
        private String deliveryType = "SERIAL";

        public SubscriptionPolicy getSubscriptionPolicy() {
            return subscriptionPolicy;
        }

        public void setSubscriptionPolicy(SubscriptionPolicy subscriptionPolicy) {
            this.subscriptionPolicy = subscriptionPolicy;
        }

        public String getDeliveryType() {
            return deliveryType;
        }

        public void setDeliveryType(String deliveryType) {
            this.deliveryType = deliveryType;
        }
    }

    public static final class SubscriptionPolicy {
        private int messageTtl = 3600;
        private int requestTimeout = 1000;

        public int getMessageTtl() {
            return messageTtl;
        }

        public void setMessageTtl(int messageTtl) {
            this.messageTtl = messageTtl;
        }

        public int getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(int requestTimeout) {
            this.requestTimeout = requestTimeout;
        }
    }

    public static final class SubscriptionDeliveryType {
        private String value = "";
        private String label = "";

        public SubscriptionDeliveryType() {
        }

        public SubscriptionDeliveryType(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static final class ConsistencyView {
        private int maxGroupBatchSize = 10;

        public int getMaxGroupBatchSize() {
            return maxGroupBatchSize;
        }

        public void setMaxGroupBatchSize(int maxGroupBatchSize) {
            this.maxGroupBatchSize = maxGroupBatchSize;
        }
    }
}
