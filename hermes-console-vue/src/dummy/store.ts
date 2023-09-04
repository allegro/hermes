import { createTestingPinia } from '@pinia/testing';
import { dummyAppConfig } from '@/dummy/app-config';
import type { AppConfigStoreState } from '@/store/app-config/types';
import type { AuthStoreState } from '@/store/auth/types';
import type { ConsistencyStoreState } from '@/store/consistency/types';
import type { FavoritesState } from '@/store/favorites/types';
import type { NotificationsState } from '@/store/app-notifications/types';

export const appConfigStoreState: AppConfigStoreState = {
  appConfig: dummyAppConfig,
  loading: false,
  error: {
    loadConfig: null,
  },
};

export const notificationsStoreState: NotificationsState = {
  notifications: [],
};

export const favoritesStoreState: FavoritesState = {
  topics: [],
  subscriptions: [],
};

export const authStoreState: AuthStoreState = {
  accessToken: '',
  codeVerifier: '',
  loading: false,
  error: {
    loadAuth: null,
  },
};

export const consistencyStoreState: ConsistencyStoreState = {
  fetchInProgress: false,
  progressPercent: 0,
  error: {
    fetchError: null,
  },
  groups: [],
};

export const dummyStoresState = {
  appConfig: appConfigStoreState,
  auth: authStoreState,
  notifications: notificationsStoreState,
  favorites: favoritesStoreState,
};

export const createTestingPiniaWithState = () =>
  createTestingPinia({ initialState: dummyStoresState });
