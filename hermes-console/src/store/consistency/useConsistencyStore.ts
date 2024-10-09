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

const partition = (array: string[], batch_size: number): string[][] => {
  const output: string[][] = [];
  for (let i = 0; i < array.length; i += batch_size) {
    output[output.length] = array.slice(i, i + batch_size);
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
    async refresh(group: string) {
      this.fetchInProgress = true;
      try {
        const refreshedGroup = (await fetchInconsistentGroups([group])).data;
        let groupIndex = -1;
        for (let i = 0; i < this.groups.length; i++) {
          if (this.groups[i].name == group) {
            groupIndex = i;
            break;
          }
        }
        if (groupIndex == -1) return;
        if (refreshedGroup.length == 0) {
          this.groups.splice(groupIndex, 1);
        } else {
          this.groups[groupIndex] = refreshedGroup[0];
        }
      } catch (e) {
        this.error.fetchError = e as Error;
      } finally {
        this.fetchInProgress = false;
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
