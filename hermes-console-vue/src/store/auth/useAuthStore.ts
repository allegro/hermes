import { encode as base64encode } from 'base64-arraybuffer';
import { defineStore } from 'pinia';
import { fetchToken } from '@/api/hermes-client';
import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
import axios from 'axios';
import decode from 'jwt-decode';
import qs from 'query-string';
import type { AuthStoreState } from '@/store/auth/types';

export const useAuthStore = defineStore('auth', {
  state: (): AuthStoreState => {
    return {
      accessToken: null,
      codeVerifier: null,
      loading: false,
      error: {
        loadAuth: null,
      },
    };
  },
  actions: {
    async exchangeCodeForTokenWithPKCE(code: string): Promise<void> {
      const configStore = useAppConfigStore();
      return fetchToken(
        code,
        `${configStore.loadedConfig.auth.oauth.url}${configStore.loadedConfig.auth.oauth.tokenEndpoint}`,
        configStore.loadedConfig.auth.oauth.clientId,
        this.getRedirectUri(),
        this.codeVerifier!!,
      )
        .then((response) => {
          this.accessToken = response.data.access_token;
          axios.interceptors.request.use(function (config) {
            config.headers.Authorization = `Bearer ${response.data.access_token}`;
            return config;
          });
        })
        .finally(() => (this.codeVerifier = null));
    },
    async getAuthorizationCodeWithPKCE(pathname: string): Promise<void> {
      const configStore = useAppConfigStore();
      this.codeVerifier = window.crypto.randomUUID();
      const queryParams = qs.stringify({
        client_id: configStore.loadedConfig.auth.oauth.clientId,
        redirect_uri: this.getRedirectUri(),
        response_type: 'code',
        code_challenge_method: 'S256',
        code_challenge: await this.generateCodeChallange(this.codeVerifier),
        state: pathname,
      });
      window.location.href = `${configStore.loadedConfig.auth.oauth.url}${configStore.loadedConfig.auth.oauth.authorizationEndpoint}?${queryParams}`;
    },
    async generateCodeChallange(codeVerifier: string): Promise<string> {
      const encoder = new TextEncoder();
      const data = encoder.encode(codeVerifier);
      const digest = await window.crypto.subtle.digest('SHA-256', data);
      const base64Digest = base64encode(digest);
      return base64Digest
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=/g, '');
    },
    getRedirectUri(): string {
      return `${window.location.origin}/ui/redirect`;
    },
    logout() {
      this.accessToken = null;
    },
    async login(path: string): Promise<void> {
      try {
        this.loading = true;
        await this.getAuthorizationCodeWithPKCE(path);
      } catch (e) {
        this.error.loadAuth = e as Error;
      } finally {
        this.loading = false;
      }
    },
  },
  getters: {
    userData(state: AuthStoreState): { exp: number } {
      return state.accessToken ? decode(state.accessToken) : { exp: 0 };
    },
    isUserAuthorized(state: AuthStoreState): boolean {
      const expiresAt = this.userData.exp * 1000;
      return state.accessToken !== null && expiresAt >= Date.now();
    },
  },
  persist: true,
});
