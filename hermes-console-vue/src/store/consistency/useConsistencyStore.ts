import { defineStore } from 'pinia';
import {
  fetchConsistencyGroups,
  fetchInconsistentGroups,
} from '@/api/hermes-client';
import type { ConsistencyStoreState } from '@/store/consistency/types';
import type {
  InconsistentGroup,
  InconsistentTopic,
} from '@/api/inconsistent-group';

const partition = (arr: string[], batch_size: number): string[][] => {
  const output: string[][] = [];
  for (let i = 0; i < arr.length; i += batch_size) {
    output[output.length] = arr.slice(i, i + batch_size);
  }
  return output;
};

const batch_size = 10;

export const useConsistencyStore = defineStore('consistency', {
  state: (): ConsistencyStoreState => {
    return {
      groups: [],
      progressPercent: 0,
      fetchInProgress: false,
      error: {
        fetchError: null,
      },
    };
  },
  actions: {
    async fetch() {
      this.groups = [];
      this.progressPercent = 0;
      this.fetchInProgress = true;
      this.error.fetchError = null;
      try {
        const groups = (await fetchConsistencyGroups()).data;
        const batches = partition(groups, batch_size);
        let processed = 0;
        for (const batch of batches) {
          const partialResult = (await fetchInconsistentGroups(batch)).data;
          processed += batch.length;
          this.groups = this.groups.concat(partialResult);
          this.progressPercent = Math.floor((processed / groups.length) * 100);
        }
      } catch (e) {
        this.error.fetchError = e as Error;
      } finally {
        this.fetchInProgress = false;
      }
    },
    group(groupName: string): InconsistentGroup | undefined {
      return this.groups.filter((g) => g.name == groupName)[0];
    },
    topic(groupName: string, topicName: string): InconsistentTopic | undefined {
      return this.group(groupName)?.inconsistentTopics.filter(
        (t) => t.name == topicName,
      )[0];
    },
    set(newState: ConsistencyStoreState) {
      this.groups = newState.groups;
      this.progressPercent = newState.progressPercent;
      this.fetchInProgress = newState.fetchInProgress;
    },
  },
  getters: {},
  persist: true,
});
