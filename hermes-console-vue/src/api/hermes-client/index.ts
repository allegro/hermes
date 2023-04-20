import type { ResponsePromise } from '@/utils/axios-utils';
import type { TopicWithSchema } from '@/api/topic';
import axios from 'axios';
import type { Owner } from '@/api/owner';

export function fetchTopic(
  topicName: string,
): ResponsePromise<TopicWithSchema> {
  return axios.get(`/topics/${topicName}`);
}

export function fetchTopicOwner(ownerId: string): ResponsePromise<Owner> {
  return axios.get(`/owners/sources/Service Catalog/${ownerId}`);
}
