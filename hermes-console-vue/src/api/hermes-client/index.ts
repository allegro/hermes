import axios from 'axios';
import type {
  MessagePreview,
  TopicMetrics,
  TopicWithSchema,
} from '@/api/topic';
import type { Owner } from '@/api/owner';
import type { ResponsePromise } from '@/utils/axios-utils';
import type { Subscription } from '@/api/subscription';

export function fetchTopic(
  topicName: string,
): ResponsePromise<TopicWithSchema> {
  return axios.get(`/topics/${topicName}`);
}

export function fetchTopicOwner(ownerId: string): ResponsePromise<Owner> {
  return axios.get(`/owners/sources/Service Catalog/${ownerId}`);
}

export function fetchTopicMessagesPreview(
  topicName: string,
): ResponsePromise<MessagePreview[]> {
  return axios.get(`/topics/${topicName}/preview`);
}

export function fetchTopicMetrics(
  topic: String,
): ResponsePromise<TopicMetrics> {
  return axios.get<TopicMetrics>(`/topics/${topic}/metrics`);
}

export function fetchTopicSubscriptions(
  topicName: string,
): ResponsePromise<string[]> {
  return axios.get(`/topics/${topicName}/subscriptions`);
}

export function fetchTopicSubscriptionDetails(
  topicName: string,
  subscription: string,
): ResponsePromise<Subscription> {
  return axios.get(`/topics/${topicName}/subscriptions/${subscription}`);
}
