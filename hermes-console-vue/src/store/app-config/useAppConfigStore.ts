import { defineStore } from 'pinia';
import { fetchAppConfiguration } from '@/api/hermes-client';
import type { AppConfigStoreState } from '@/store/app-config/types';
import type { AppConfiguration } from '@/api/app-configuration';

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
  getters: {
    loadedConfig(state: AppConfigStoreState): AppConfiguration {
      return state.appConfig!!;
    },
  },
});
