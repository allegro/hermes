import { createTestingPinia } from '@pinia/testing';
import { dummyAppConfig } from '@/dummy/app-config';
import type { AppConfigStoreState } from '@/store/types';

export const appConfigStoreState: AppConfigStoreState = {
  appConfig: dummyAppConfig,
  loading: false,
  error: {
    loadConfig: null,
  },
};

export const dummyStoresState = {
  appConfig: appConfigStoreState,
};

export const createTestingPiniaWithState = () =>
  createTestingPinia({ initialState: dummyStoresState });
