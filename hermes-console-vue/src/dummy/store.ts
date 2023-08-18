import { createTestingPinia } from '@pinia/testing';
import { dummyAppConfig } from '@/dummy/app-config';
import type { AppConfigStoreState } from '@/store/app-config/types';
import type { AuthStoreState } from '@/store/auth/types';

export const appConfigStoreState: AppConfigStoreState = {
  appConfig: dummyAppConfig,
  loading: false,
  error: {
    loadConfig: null,
  },
};

export const authStoreState: AuthStoreState = {
  accessToken: '',
  codeVerifier: '',
  loading: false,
  error: {
    loadAuth: null,
  },
};

export const dummyStoresState = {
  appConfig: appConfigStoreState,
  auth: authStoreState,
};

export const createTestingPiniaWithState = () =>
  createTestingPinia({ initialState: dummyStoresState });
