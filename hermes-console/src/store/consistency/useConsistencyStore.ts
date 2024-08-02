import { defineStore } from 'pinia';
import {
  fetchInconsistentGroups,
} from '@/api/hermes-client';
import type { ConsistencyStoreState } from '@/store/consistency/types';
import type {
  InconsistentGroup,
  InconsistentTopic,
} from '@/api/inconsistent-group';


export const useConsistencyStore = defineStore('consistency', {
  state: (): ConsistencyStoreState => {
    return {
      groups: [],
      error: {
        fetchError: null,
      },
    };
  },
  actions: {
    async fetch() {
      this.groups = [];
      this.error.fetchError = null;
      try {
        this.groups = (await fetchInconsistentGroups()).data;
      } catch (e) {
        this.error.fetchError = e as Error;
      }
    },
  },
  getters: {
    group(
      state: ConsistencyStoreState,
    ): (groupName: string) => InconsistentGroup | undefined {
      return (groupName: string) =>
        state.groups.filter((g) => g.name == groupName)[0];
    },
    topic(): (
      groupName: string,
      topicName: string,
    ) => InconsistentTopic | undefined {
      return (groupName: string, topicName: string) =>
        this.group(groupName)?.inconsistentTopics.filter(
          (t) => t.name == topicName,
        )[0];
    },
  },
  persist: true,
});
