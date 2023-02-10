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
}

type TopicName = string;

interface RetentionTime {
  duration: number;
  retentionUnit: string; // java.util.concurrent.TimeUnit
}

enum Ack {
  NONE,
  LEADER,
  ALL,
}

interface PublishingAuth {
  publishers: string[];
  enabled: boolean;
  unauthenticatedAccessEnabled: boolean;
}

interface TopicDataOfflineStorage {
  enabled: boolean;
  retentionTime: OfflineRetentionTime;
}

interface OfflineRetentionTime {
  duration: number;
  infinite: boolean;
}

interface TopicLabel {
  value: string;
}
