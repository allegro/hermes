import { afterEach } from 'vitest';
import { dummyInconsistentTopics } from '@/dummy/inconsistentTopics';
import {
  fetchInconsistentTopicsErrorHandler,
  fetchInconsistentTopicsHandler,
} from '@/mocks/handlers';
import { setupServer } from 'msw/node';
import { useInconsistentTopics } from '@/composables/inconsistent-topics/use-inconsistent-topics/useInconsistentTopics';
import { waitFor } from '@testing-library/vue';

describe('useInconsistentTopics', () => {
  const server = setupServer(
    fetchInconsistentTopicsHandler({ topics: dummyInconsistentTopics }),
  );

  afterEach(() => {
    server.resetHandlers();
  });

  it('should fetch topics consistency details from Hermes API', async () => {
    // given
    server.listen();

    // when
    const { topics, loading, error } = useInconsistentTopics();

    // then
    expect(loading.value).toBe(true);

    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value.fetchInconsistentTopics).toBe(null);
      expect(topics.value).toEqual(
        expect.arrayContaining(dummyInconsistentTopics),
      );
    });
  });

  it('should set error to true on topics consistency endpoint failure', async () => {
    // given
    server.use(fetchInconsistentTopicsErrorHandler({ errorCode: 500 }));
    server.listen();

    // when
    const { loading, error } = useInconsistentTopics();

    // then
    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value.fetchInconsistentTopics).not.toBeNull();
    });
  });
});
