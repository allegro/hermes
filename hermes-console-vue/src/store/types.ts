import type { AppConfiguration } from '@/api/app-configuration';

export interface AppConfigStoreState {
  appConfig: AppConfiguration | undefined;
  loading: boolean;
  error: AppConfigStoreErrors;
}

export interface AppConfigStoreErrors {
  loadConfig: Error | null;
}
