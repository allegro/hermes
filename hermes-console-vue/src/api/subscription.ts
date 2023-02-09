/* eslint-disable no-unused-vars */
import type { ContentType } from '@/api/content-type';
import type { OwnerId } from '@/api/owner-id';

export interface Subscription {
  topicName: string;
  name: string;
  endpoint: EndpointAddress;
  state: State;
  description: string;
  subscriptionPolicy: Record<string, any>;
  trackingEnabled: boolean;
  trackingMode: string;
  owner: OwnerId;
  monitoringDetails: MonitoringDetails;
  contentType: ContentType;
  deliveryType: DeliveryType;
  filters: MessageFilterSpecification[];
  mode: SubscriptionMode;
  headers: Header[];
  endpointAddressResolverMetadata: EndpointAddressResolverMetadata;
  oAuthPolicy?: SubscriptionOAuthPolicy; // nullable? (missing in response)
  http2Enabled: boolean;
  subscriptionIdentityHeadersEnabled: boolean;
  autoDeleteWithTopicEnabled: boolean;
  createdAt: number; // java.time.Instant
  modifiedAt: number; // java.time.Instant
}

type EndpointAddress = string;

const enum State {
  PENDING,
  ACTIVE,
  SUSPENDED,
}

interface MonitoringDetails {
  severity: Severity;
  reaction: string;
}

const enum Severity {
  CRITICAL,
  IMPORTANT,
  NON_IMPORTANT,
}

const enum DeliveryType {
  SERIAL,
  BATCH,
}

type MessageFilterSpecification = Record<string, any>;

const enum SubscriptionMode {
  ANYCAST,
  BROADCAST,
}

interface Header {
  name: string;
  value: string;
}

type EndpointAddressResolverMetadata = Record<string, any>;

interface SubscriptionOAuthPolicy {
  grantType: GrantType;
  providerName: string;
  scope: string;
  username: string;
  password: string;
}

const enum GrantType {
  CLIENT_CREDENTIALS,
  USERNAME_PASSWORD,
}
