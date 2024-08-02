import type { InconsistentGroup } from '@/api/inconsistent-group';

export interface ConsistencyStoreState {
  groups: InconsistentGroup[];
  error: ConsistencyFetchError;
}

export interface ConsistencyFetchError {
  fetchError: Error | null;
}
