import { defineStore } from 'pinia';
import { fetchAppConfiguration } from '@/api/hermes-client';
import type { AppConfigStoreState } from '@/store/types';

export const useAppConfigStore = defineStore('appConfig', {
  state: (): AppConfigStoreState => {
    return {
      appConfig: undefined,
      loading: false,
      error: {
        loadConfig: null,
      },
    };
  },
  actions: {
    async loadConfig() {
      try {
        this.loading = true;
        this.appConfig = (await fetchAppConfiguration()).data;
      } catch (e) {
        this.error.loadConfig = e as Error;
      } finally {
        this.loading = false;
      }
    },
  },
});
