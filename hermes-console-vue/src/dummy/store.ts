import { dummyAppConfig } from '@/dummy/app-config';
import { createTestingPinia } from '@pinia/testing';

export const appConfigStoreState = {
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
