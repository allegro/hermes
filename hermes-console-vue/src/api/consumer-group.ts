/* eslint-disable no-unused-vars */
import type { ContentType } from '@/api/content-type';

export interface ConsumerGroup {
  clusterName: string;
  groupId: string;
  state: ConsumerGroupState;
  members: ConsumerGroupMember[];
}

export interface ConsumerGroupMember {
  consumerId: string;
  clientId: string;
  host: string;
  partitions: TopicPartition[];
}

export interface TopicPartition {
  partition: number;
  topic: string;
  currentOffset: number;
  logEndOffset: number;
  lag: number;
  offsetMetadata: string;
  contentType: ContentType;
}

export const enum ConsumerGroupState {
  UNKNOWN = 'Unknown',
  PREPARING_REBALANCE = 'PreparingRebalance',
  COMPLETING_REBALANCE = 'CompletingRebalance',
  STABLE = 'Stable',
  DEAD = 'Dead',
  EMPTY = 'Empty',
}
