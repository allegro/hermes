import { createPinia, setActivePinia } from 'pinia';
import { dummyAppConfig } from '@/dummy/app-config';
import { dummyValidToken } from '@/dummy/jwt-tokens';
import { expect } from 'vitest';
import { fetchTokenHandler } from '@/mocks/handlers';
import { setupServer } from 'msw/node';
import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
import { useAuthStore } from '@/store/auth/useAuthStore';

describe('useGroups', () => {
  const server = setupServer();

  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('should initialize', async () => {
    // when
    const authStore = useAuthStore();

    // then
    expect(authStore.accessToken).toBeNull();
    expect(authStore.codeVerifier).toBeNull();
    expect(authStore.loading).toBeFalsy();
    expect(authStore.error.loadAuth).toBeNull();
    expect(authStore.isUserAuthorized).toBeFalsy();
  });

  it('should generate codeVerifier', async () => {
    //given
    const authStore = useAuthStore();

    // when
    const promise = authStore.login('/');

    // then
    expect(authStore.loading).toBeTruthy();

    // when
    await promise;

    // then
    expect(authStore.loading).toBeFalsy();
    expect(authStore.codeVerifier).not.toBeNull();
  });

  it('should exchange code for token', async () => {
    //given
    server.use(
      fetchTokenHandler({ accessToken: { access_token: dummyValidToken } }),
    );
    server.listen();
    const configStore = useAppConfigStore();
    configStore.appConfig = dummyAppConfig;
    const authStore = useAuthStore();

    // when
    await authStore.exchangeCodeForTokenWithPKCE('codeXYZ');

    // then
    expect(authStore.accessToken).toBe(dummyValidToken);
    expect(authStore.codeVerifier).toBeNull();
    expect(authStore.isUserAuthorized).toBeTruthy();
  });
});
