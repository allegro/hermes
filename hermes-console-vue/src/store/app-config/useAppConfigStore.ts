import { defineStore } from 'pinia';
import type { AppConfigStoreState } from '@/store/types';
import { fetchAppConfiguration } from '@/api/hermes-client';

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
        // this.appConfig = (await fetchAppConfiguration()).data;
        throw new Error('sss');
      } catch (e) {
        this.error.loadConfig = e as Error;
      } finally {
        this.loading = false;
      }
    },
  },
});
