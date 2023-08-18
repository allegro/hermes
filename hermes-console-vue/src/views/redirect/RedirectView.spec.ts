import { createTestingPinia } from '@pinia/testing';
import { expect } from 'vitest';
import { render } from '@/utils/test-utils';
import { setActivePinia } from 'pinia';
import { useAuthStore } from '@/store/auth/useAuthStore';
import RedirectView from '@/views/redirect/RedirectView.vue';

describe('RedirectView', () => {
  const pinia = createTestingPinia({ createSpy: vi.fn });

  beforeEach(() => {
    setActivePinia(pinia);
  });

  it('should push to state', () => {
    // when
    const authStore = useAuthStore();
    render(RedirectView, { testPinia: pinia });

    // then
    expect(authStore.exchangeCodeForTokenWithPKCE).toHaveBeenCalledOnce();
  });
});
