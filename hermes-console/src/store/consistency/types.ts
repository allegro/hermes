import type { InconsistentGroup } from '@/api/inconsistent-group';

export interface ConsistencyStoreState {
  groups: InconsistentGroup[];
  progressPercent: number;
  fetchInProgress: boolean;
  error: ConsistencyFetchError;
}

export interface ConsistencyFetchError {
  fetchError: Error | null;
}
