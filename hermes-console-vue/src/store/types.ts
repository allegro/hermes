import type { AppConfiguration } from '@/api/app-configuration';

export interface AppConfigStoreState {
  appConfig?: AppConfiguration;
  loading: boolean;
  error: AppConfigStoreErrors;
}

export interface AppConfigStoreErrors {
  loadConfig: Error | null;
}
