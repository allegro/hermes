import { createPinia, setActivePinia } from 'pinia';
import { dummyAppConfig } from '@/dummy/app-config';
import { expect } from 'vitest';
import { fetchTokenHandler } from '@/mocks/handlers';
import { setupServer } from 'msw/node';
import { useAppConfigStore } from '@/store/app-config/useAppConfigStore';
import { useAuthStore } from '@/store/auth/useAuthStore';
import { validToken } from '@/utils/jwt-utils';

describe('useGroups', () => {
  const server = setupServer();

  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('should initialize', async () => {
    // when
    const authStoreState = useAuthStore();

    // then
    expect(authStoreState.accessToken).toBeNull();
    expect(authStoreState.codeVerifier).toBeNull();
    expect(authStoreState.loading).toBeFalsy();
    expect(authStoreState.error.loadAuth).toBeNull();
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
      fetchTokenHandler({ accessToken: { access_token: validToken } }),
    );
    server.listen();
    const configStore = useAppConfigStore();
    configStore.appConfig = dummyAppConfig;
    const authStore = useAuthStore();

    // when
    await authStore.exchangeCodeForTokenWithPKCE('codeXYZ');

    // then
    expect(authStore.accessToken).toBe(validToken);
    expect(authStore.codeVerifier).toBeNull();
  });
});
