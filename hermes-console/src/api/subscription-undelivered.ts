/* eslint-disable no-unused-vars */
export interface SentMessageTrace {
  messageId?: string;
  batchId?: string;
  subscription: string;
  timestamp: number;
  status: SentMessageTraceStatus;
  partition: number;
  offset: number;
  topicName: TopicName;
  reason: string;
  message: string;
  cluster: string;
}

export const enum SentMessageTraceStatus {
  INFLIGHT = 'INFLIGHT',
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  DISCARDED = 'DISCARDED',
  FILTERED = 'FILTERED',
}

export type TopicName = string;
