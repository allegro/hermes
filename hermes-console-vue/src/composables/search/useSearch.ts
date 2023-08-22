import { querySubscriptions, queryTopics } from '@/api/hermes-client';
import { ref } from 'vue';
import type { Ref } from 'vue';
import type { Subscription } from '@/api/subscription';
import type { Topic } from '@/api/topic';

export interface UseSearch {
  topics: Ref<Topic[] | undefined>;
  subscriptions: Ref<Subscription[] | undefined>;
  queryTopicsFn: (filter: SearchFilter, pattern: string) => void;
  querySubscriptionsFn: (filter: SearchFilter, pattern: string) => void;
  loading: Ref<boolean>;
  error: Ref<UseSearchErrors>;
}

export interface UseSearchErrors {
  fetchError: Error | null;
}

export enum SearchFilter {
  NAME = 'name',
  OWNER = 'owner.id',
  ENDPOINT = 'endpoint',
}

export function useSearch(): UseSearch {
  const topics = ref<Topic[]>();
  const subscriptions = ref<Subscription[]>();
  const error = ref<UseSearchErrors>({
    fetchError: null,
  });
  const loading = ref(false);

  const queryTopicsFn = async (filter: SearchFilter, pattern: string) => {
    try {
      loading.value = true;
      const query = buildQuery(filter, pattern);
      topics.value = (await queryTopics(query)).data;
    } catch (e) {
      error.value.fetchError = e as Error;
    } finally {
      loading.value = false;
    }
  };

  const querySubscriptionsFn = async (
    filter: SearchFilter,
    pattern: string,
  ) => {
    try {
      loading.value = true;
      const query = buildQuery(filter, pattern);
      subscriptions.value = (await querySubscriptions(query)).data;
    } catch (e) {
      error.value.fetchError = e as Error;
    } finally {
      loading.value = false;
    }
  };

  return {
    topics,
    subscriptions,
    queryTopicsFn,
    querySubscriptionsFn,
    loading,
    error,
  };
}

function buildQuery(filter: SearchFilter, pattern: string): Object {
  return {
    query: {
      [filter]: {
        like: pattern,
      },
    },
  };
}
