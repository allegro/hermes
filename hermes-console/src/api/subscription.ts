/* eslint-disable no-unused-vars */
import type { ContentType } from '@/api/content-type';
import type { OwnerId } from '@/api/owner-id';

export interface Subscription {
  topicName: string;
  name: string;
  endpoint: string;
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

export interface CreateSubscriptionFormRequestBody {
  name: string;
  topicName: string;
  owner: OwnerJson;
  contentType: string;
  deliveryType: string;
  description: string;
  endpoint?: string;
  filters: SubscriptionFilterJson[];
  headers: SubscriptionHeaderJson[];
  http2Enabled: boolean;
  mode: string;
  monitoringDetails: MonitoringDetailsJson;
  subscriptionPolicy: SubscriptionPolicyJson;
  trackingMode: string;
  endpointAddressResolverMetadata: EndpointAddressResolverMetadataJson;
  subscriptionIdentityHeadersEnabled: boolean;
  autoDeleteWithTopicEnabled: boolean;
}

interface OwnerJson {
  source: string;
  id: string;
}

interface HeaderFilterJson {
  type: string;
  header: string;
  matcher: string;
}

export interface PathFilterJson {
  type: string;
  path: string;
  matcher: string;
  matchingStrategy: string;
}

export type SubscriptionFilterJson = HeaderFilterJson | PathFilterJson;

interface SubscriptionHeaderJson {}

interface MonitoringDetailsJson {
  reaction: string;
  severity: string;
}

export interface SerialSubscriptionPolicyJson {
  backoffMaxIntervalInSec: number;
  backoffMultiplier: number;
  messageBackoff: number;
  messageTtl: number;
  inflightSize: number;
  rate: number;
  requestTimeout: number;
  sendingDelay: number;
  retryClientErrors: boolean;
}

export interface BatchSubscriptionPolicyJson {
  messageTtl: number;
  retryClientsErrors: boolean;
  messageBackoff: number;
  requestTimeout: number;
  batchSize: number;
  batchTime: number;
  batchVolume: number;
}

export type SubscriptionPolicyJson =
  | SerialSubscriptionPolicyJson
  | BatchSubscriptionPolicyJson;

export type EndpointAddressResolverMetadataJson = Record<string, any>;
