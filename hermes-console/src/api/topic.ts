/* eslint-disable no-unused-vars */
import type { ContentType } from '@/api/content-type';
import type { OwnerId } from '@/api/owner-id';

export interface TopicWithSchema extends Topic {
  schema: string;
}

export interface Topic {
  name: TopicName;
  description: string;
  owner: OwnerId;
  retentionTime: RetentionTime;
  jsonToAvroDryRun: boolean;
  ack: Ack;
  trackingEnabled: boolean;
  migratedFromJsonType: boolean;
  schemaIdAwareSerializationEnabled: boolean;
  contentType: ContentType;
  maxMessageSize?: number;
  auth: PublishingAuth;
  subscribingRestricted: boolean;
  offlineStorage: TopicDataOfflineStorage;
  labels: TopicLabel[];
  createdAt: number; // java.time.Instant
  modifiedAt: number; // java.time.Instant
  fallbackToRemoteDatacenterEnabled: boolean;
}

export type TopicName = string;

export interface RetentionTime {
  duration: number;
  retentionUnit: string; // java.util.concurrent.TimeUnit
}

export enum Ack {
  NONE = 'NONE',
  LEADER = 'LEADER',
  ALL = 'ALL',
}

export interface PublishingAuth {
  publishers?: string[];
  enabled: boolean;
  unauthenticatedAccessEnabled: boolean;
}

export interface TopicDataOfflineStorage {
  enabled: boolean;
  retentionTime: OfflineRetentionTime;
}

export interface OfflineRetentionTime {
  duration: number;
  infinite: boolean;
}

export interface TopicLabel {
  value: string;
}

export interface TopicMetrics {
  published: number;
  volume: number;
  rate: string;
  deliveryRate: string;
  subscriptions: number;
  throughput: string;
}

export interface MessagePreview {
  content: string;
  truncated: boolean;
}
