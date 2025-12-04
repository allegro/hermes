import { defineStore } from 'pinia';
import type { FeatureFlagsState } from '@/store/feature-flags/types';

export const useFeatureFlagsStore = defineStore('featureFlags', {
  state: (): FeatureFlagsState => {
    return {
      searchV2Enabled: false,
    };
  },
  actions: {
    async setSearchV2Enabled(enabled: boolean) {
      this.searchV2Enabled = enabled;
    },
  },
  persist: true,
});
