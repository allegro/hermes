export interface AppConfiguration {
  console: ConsoleConfiguration;
  dashboard: DashboardConfiguration;
  hermes: HermesConfiguration;
  metrics: MetricsConfiguration;
  auth: AuthConfiguration;
  owner: OwnerConfiguration;
  topic: TopicViewConfiguration;
  subscription: SubscriptionViewConfiguration;
  consistency: ConsistencyViewConfiguration;
  group: GroupViewConfiguration;
}

export interface ConsoleConfiguration {
  title: string;
  footer: string;
  environmentName: string;
  criticalEnvironment: boolean;
}

export interface DashboardConfiguration {
  metrics: string;
  docs: string;
}

export interface HermesConfiguration {
  discovery: DiscoveryConfiguration;
}

export interface DiscoveryConfiguration {
  type: string;
  simple: SimpleDiscoveryConfiguration;
}

export interface SimpleDiscoveryConfiguration {
  url: string;
}

export interface MetricsConfiguration {
  type: string;
  graphite: GraphiteMetricsConfiguration;
}

export interface GraphiteMetricsConfiguration {
  url: string;
  prefix: string;
}

export interface AuthConfiguration {
  oauth: OAuthConfiguration;
  headers: AuthHeadersConfiguration;
}

export interface OAuthConfiguration {
  enabled: boolean;
  url: string;
  clientId: string;
  scope: string;
}

export interface AuthHeadersConfiguration {
  enabled: boolean;
  adminHeader: string;
}

export interface OwnerConfiguration {
  sources: OwnerSourceConfiguration[];
}

export interface OwnerSourceConfiguration {
  name: string;
  placeholder: string;
}

export interface TopicViewConfiguration {
  messagePreviewEnabled: boolean;
  offlineClientsEnabled: boolean;
  authEnabled: boolean;
  defaults: DefaultTopicViewConfiguration;
  buttonsExtension: string;
  removeSchema: boolean;
  schemaIdAwareSerializationEnabled: boolean;
  avroContentTypeMetadataRequired: boolean;
  contentTypes: TopicContentType[];
  readOnlyModeEnabled: boolean;
  allowedTopicLabels: string[];
  retentionUnits: RetentionUnit[];
  offlineRetransmissionEnabled: boolean;
  offlineRetransmissionDescription: string;
}

export interface DefaultTopicViewConfiguration {
  ack: string;
  contentType: string;
  retentionTime: RetentionTimeConfiguration;
  offlineStorage: DefaultOfflineStorageViewConfiguration;
}

export interface DefaultOfflineStorageViewConfiguration {
  enabled: boolean;
  retentionTime: RetentionTimeConfiguration;
}

export interface RetentionTimeConfiguration {
  duration: number;
  retentionUnit: string;
}

export interface RetentionUnit {
  value: string;
  label: string;
}

export interface TopicContentType {
  value: string;
  label: string;
}

export interface SubscriptionViewConfiguration {
  endpointAddressResolverMetadata: Record<
    string,
    EndpointAddressResolverMetadataConfiguration
  >;
  showHeadersFilter: boolean;
  showFixedHeaders: boolean;
  requestTimeoutWarningThreshold: number;
  defaults: DefaultSubscriptionViewConfiguration;
  deliveryTypes: SubscriptionDeliveryType[];
}

export interface EndpointAddressResolverMetadataConfiguration {
  title: string;
  type: string;
  hint: string;
}

export interface DefaultSubscriptionViewConfiguration {
  subscriptionPolicy: SubscriptionPolicyConfiguration;
  deliveryType: string;
}

export interface SubscriptionPolicyConfiguration {
  messageTtl: number;
  requestTimeout: number;
}

export interface SubscriptionDeliveryType {
  value: string;
  label: string;
}

export interface ConsistencyViewConfiguration {
  maxGroupBatchSize: number;
}

export interface GroupViewConfiguration {
  nonAdminCreationEnabled: boolean;
}
