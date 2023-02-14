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
  createdAt: number;
  modifiedAt: number;
}

export type EndpointAddress = string;

export const enum State {
  PENDING = 'PENDING',
  ACTIVE = 'ACTIVE',
  SUSPENDED = 'SUSPENDED',
}

export interface MonitoringDetails {
  severity: Severity;
  reaction: string;
}

export const enum Severity {
  CRITICAL = 'CRITICAL',
  IMPORTANT = 'IMPORTANT',
  NON_IMPORTANT = 'NON_IMPORTANT',
}

export const enum DeliveryType {
  SERIAL = 'SERIAL',
  BATCH = 'BATCH',
}

export type MessageFilterSpecification = Record<string, any>;

export const enum SubscriptionMode {
  ANYCAST = 'ANYCAST',
  BROADCAST = 'BROADCAST',
}

export interface Header {
  name: string;
  value: string;
}

export type EndpointAddressResolverMetadata = Record<string, any>;

export interface SubscriptionOAuthPolicy {
  grantType: GrantType;
  providerName: string;
  scope: string;
  username: string;
  password: string;
}

export const enum GrantType {
  CLIENT_CREDENTIALS = 'CLIENT_CREDENTIALS',
  USERNAME_PASSWORD = 'USERNAME_PASSWORD',
}
