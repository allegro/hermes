export interface AuthStoreState {
  accessToken: string | null;
  codeVerifier: string | null;
  loading: boolean;
  error: AuthStoreErrors;
}

export interface AuthStoreErrors {
  loadAuth: Error | null;
}
