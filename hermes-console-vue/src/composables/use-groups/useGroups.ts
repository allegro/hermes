import { computed, ref } from 'vue';
import axios from 'axios';

interface Group {
  name: string;
  topics: string[];
}

export function useGroups() {
  const groupNames = ref<string[]>();
  const topicNames = ref<string[]>();
  const error = ref<boolean>(false);

  const loading = computed(
    () => !error.value && !(groupNames.value && topicNames.value),
  );

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

  function fetchGroupNames() {
    axios
      .get<string[]>('/groups')
      .then((response) => (groupNames.value = response.data))
      .catch(() => (error.value = true));
  }

  function fetchTopicNames() {
    axios
      .get<string[]>('/topics')
      .then((response) => (topicNames.value = response.data))
      .catch(() => (error.value = true));
  }

  fetchGroupNames();
  fetchTopicNames();

  return {
    groups,
    loading,
    error,
  };
}
