import axios from 'axios';
import qs from 'query-string';
import type { AccessTokenResponse } from '@/api/access-token-response';
import type { AppConfiguration } from '@/api/app-configuration';
import type { AxiosRequestConfig } from 'axios';
import type { ConstraintsConfig } from '@/api/constraints';
import type { ConsumerGroup } from '@/api/consumer-group';
import type {
  CreateSubscriptionFormRequestBody,
  Subscription,
} from '@/api/subscription';
import type { DatacenterReadiness } from '@/api/datacenter-readiness';
import type {
  MessagePreview,
  TopicMetrics,
  TopicWithSchema,
} from '@/api/topic';
import type { OfflineClientsSource } from '@/api/offline-clients-source';
import type { Owner, OwnerSource } from '@/api/owner';
import type { ResponsePromise } from '@/utils/axios-utils';
import type { Stats } from '@/api/stats';

const acceptHeader = 'Accept';
const contentTypeHeader = 'Content-Type';
const applicationJsonMediaType = 'application/json';

export class HermesClient {
  createSubscription(
    topic: string,
    requestBody: CreateSubscriptionFormRequestBody,
  ): ResponsePromise<void> {
    return axios.post(`/topics/${topic}/subscriptions`, requestBody, {
      headers: {
        [acceptHeader]: applicationJsonMediaType,
        [contentTypeHeader]: applicationJsonMediaType,
      },
    });
  }
}

export const hermesClient = new HermesClient();

export function fetchTopic(
  topicName: string,
): ResponsePromise<TopicWithSchema> {
  return axios.get<TopicWithSchema>(`/topics/${topicName}`);
}

export function fetchOwnersSources(): ResponsePromise<OwnerSource[]> {
  return axios.get<OwnerSource[]>(`/owners/sources`);
}

export function searchOwners(
  source: string,
  searchPhrase: string,
): ResponsePromise<Owner[]> {
  return axios.get<Owner[]>(`/owners/sources/${source}?search=${searchPhrase}`);
}

export function fetchTopicOwner(ownerId: string): ResponsePromise<Owner> {
  return axios.get<Owner>(`/owners/sources/Service Catalog/${ownerId}`);
}

export function fetchTopicMessagesPreview(
  topicName: string,
): ResponsePromise<MessagePreview[]> {
  return axios.get<MessagePreview[]>(`/topics/${topicName}/preview`);
}

export function fetchTopicMetrics(
  topic: String,
): ResponsePromise<TopicMetrics> {
  return axios.get<TopicMetrics>(`/topics/${topic}/metrics`);
}

export function fetchTopicSubscriptions(
  topicName: string,
): ResponsePromise<string[]> {
  return axios.get<string[]>(`/topics/${topicName}/subscriptions`);
}

export function fetchTopicSubscriptionDetails(
  topicName: string,
  subscription: string,
): ResponsePromise<Subscription> {
  return axios.get<Subscription>(
    `/topics/${topicName}/subscriptions/${subscription}`,
  );
}

export function fetchAppConfiguration(): ResponsePromise<AppConfiguration> {
  return axios.get<AppConfiguration>('/console', {
    headers: { accept: 'application/json' },
  });
}

export function fetchOfflineClientsSource(
  topicName: string,
): ResponsePromise<OfflineClientsSource> {
  return axios.get<OfflineClientsSource>(
    `/topics/${topicName}/offline-clients-source`,
  );
}

export function fetchConstraints(): ResponsePromise<ConstraintsConfig> {
  return axios.get<ConstraintsConfig>('/workload-constraints');
}

export function fetchReadiness(): ResponsePromise<DatacenterReadiness[]> {
  return axios.get<DatacenterReadiness[]>('/readiness/datacenters');
}

export function fetchConsumerGroups(
  topicName: string,
  subscription: string,
): ResponsePromise<ConsumerGroup[]> {
  return axios.get<ConsumerGroup[]>(
    `/topics/${topicName}/subscriptions/${subscription}/consumer-groups`,
  );
}

export function fetchInconsistentTopics(): ResponsePromise<string[]> {
  return axios.get<string[]>('/consistency/inconsistencies/topics');
}

export function fetchTopicNames(): ResponsePromise<string[]> {
  return axios.get<string[]>('/topics');
}

export function fetchGroupNames(): ResponsePromise<string[]> {
  return axios.get<string[]>('/groups');
}

export function fetchStats(): ResponsePromise<Stats> {
  return axios.get<Stats>(`/stats`);
}

export function fetchToken(
  code: string,
  url: string,
  clientId: string,
  redirectUri: string,
  codeVerifier: string,
): ResponsePromise<AccessTokenResponse> {
  return axios.post<AccessTokenResponse>(
    url,
    qs.stringify({
      client_id: clientId,
      code: code,
      redirect_uri: redirectUri,
      grant_type: 'authorization_code',
      code_verifier: codeVerifier,
    }),
    {
      'Content-Type': 'application/x-www-form-urlencoded',
    } as AxiosRequestConfig,
  );
}
