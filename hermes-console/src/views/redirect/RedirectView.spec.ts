import { appConfigStoreState } from '@/dummy/store';
import { createTestingPinia } from '@pinia/testing';
import { dummyValidToken } from '@/dummy/jwt-tokens';
import { expect } from 'vitest';
import { fetchTokenHandler } from '@/mocks/handlers';
import { render } from '@/utils/test-utils';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useAuthStore } from '@/store/auth/useAuthStore';
import RedirectView from '@/views/redirect/RedirectView.vue';
import router from '@/router';

describe('RedirectView', () => {
  const server = setupServer();
  const pinia = createTestingPinia({
    createSpy: vi.fn,
    initialState: { appConfig: appConfigStoreState },
  });

  beforeEach(() => {
    setActivePinia(pinia);
  });

  it('should push to state', async () => {
    // given
    server.use(
      fetchTokenHandler({ accessToken: { access_token: dummyValidToken } }),
    );
    server.listen();
    const authStore = useAuthStore();
    vi.mocked(authStore.exchangeCodeForTokenWithPKCE).mockReturnValueOnce(
      Promise.resolve(),
    );
    await router.push(`/ui/redirect?code=XYZ`);

    // when
    render(RedirectView, { testPinia: pinia });

    // then
    expect(authStore.exchangeCodeForTokenWithPKCE).toHaveBeenCalledOnce();
  });
});
