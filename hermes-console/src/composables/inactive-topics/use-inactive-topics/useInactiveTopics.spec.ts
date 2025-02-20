import { afterEach, expect } from 'vitest';
import { createTestingPinia } from '@pinia/testing';
import { dummyInactiveTopics } from '@/dummy/inactiveTopics';
import {
  fetchInactiveTopicsErrorHandler,
  fetchInactiveTopicsHandler,
} from '@/mocks/handlers';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useInactiveTopics } from '@/composables/inactive-topics/use-inactive-topics/useInactiveTopics';
import { waitFor } from '@testing-library/vue';

describe('useInactiveTopics', () => {
  const server = setupServer(
    fetchInactiveTopicsHandler({ inactiveTopics: dummyInactiveTopics }),
  );

  const pinia = createTestingPinia({
    fakeApp: true,
  });

  beforeEach(() => {
    setActivePinia(pinia);
  });

  afterEach(() => {
    server.resetHandlers();
  });

  it('should fetch inactive topics from Hermes backend', async () => {
    // given
    server.listen();

    // when
    const { inactiveTopics, loading, error } = useInactiveTopics();

    // then
    expect(loading.value).toBeTruthy();
    expect(error.value.fetchInactiveTopics).toBeNull();

    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchInactiveTopics).toBeNull();
      expect(inactiveTopics.value?.length).toBe(2);
    });
  });

  it('should set error to true on inactive topics endpoint failure', async () => {
    // given
    server.use(fetchInactiveTopicsErrorHandler({ errorCode: 500 }));
    server.listen();

    // when
    const { error } = useInactiveTopics();

    // then
    await waitFor(() => {
      expect(error.value.fetchInactiveTopics).not.toBeNull();
    });
  });
});
