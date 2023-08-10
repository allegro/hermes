import { computed, ref } from 'vue';
import { fetchGroupNames as getGroupNames } from '@/api/hermes-client';
import { fetchTopicNames as getTopicNames } from '@/api/hermes-client';
import type { Ref } from 'vue';

export interface UseGroups {
  groups: Ref<Group[] | undefined>;
  loading: Ref<boolean>;
  error: Ref<UseGroupsErrors>;
}

export interface UseGroupsErrors {
  fetchGroupNames: Error | null;
  fetchTopicNames: Error | null;
}

export interface Group {
  name: string;
  topics: string[];
}

export function useGroups(): UseGroups {
  const groupNames = ref<string[]>();
  const topicNames = ref<string[]>();
  const error = ref<UseGroupsErrors>({
    fetchGroupNames: null,
    fetchTopicNames: null,
  });
  const loading = ref(false);

  const groups = computed((): Group[] | undefined => {
    return groupNames.value
      ?.map((groupName) => ({
        name: groupName,
        topics:
          topicNames.value?.filter(
            (topicName) =>
              topicName.indexOf(groupName) === 0 &&
              groupName.length === topicName.lastIndexOf('.'),
          ) ?? [],
      }))
      .sort(({ name: a }, { name: b }) => a.localeCompare(b));
  });

  const fetchGroupNames = async () => {
    try {
      loading.value = true;
      groupNames.value = (await getGroupNames()).data;
    } catch (e) {
      error.value.fetchGroupNames = e as Error;
    } finally {
      loading.value = false;
    }
  };

  const fetchTopicNames = async () => {
    try {
      loading.value = true;
      topicNames.value = (await getTopicNames()).data;
    } catch (e) {
      error.value.fetchTopicNames = e as Error;
    } finally {
      loading.value = false;
    }
  };

  fetchGroupNames();
  fetchTopicNames();

  return {
    groups,
    loading,
    error,
  };
}
