import { afterEach } from 'vitest';
import { dummyDatacentersReadiness } from '@/dummy/readiness';
import {
  fetchReadinessErrorHandler,
  fetchReadinessHandler,
} from '@/mocks/handlers';
import { setupServer } from 'msw/node';
import { useReadiness } from '@/composables/readiness/use-readiness/useReadiness';
import { waitFor } from '@testing-library/vue';

describe('useReadiness', () => {
  const server = setupServer(
    fetchReadinessHandler({ datacentersReadiness: dummyDatacentersReadiness }),
  );

  afterEach(() => {
    server.resetHandlers();
  });

  it('should fetch readiness details from Hermes API', async () => {
    // given
    server.listen();

    // when
    const { datacentersReadiness, loading, error } = useReadiness();

    // then
    expect(loading.value).toBe(true);

    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value.fetchReadiness).toBe(null);
      expect(datacentersReadiness.value).toEqual(dummyDatacentersReadiness);
    });
  });

  it('should set error to true on datacenters readiness endpoint failure', async () => {
    // given
    server.use(fetchReadinessErrorHandler({ errorCode: 500 }));
    server.listen();

    // when
    const { loading, error } = useReadiness();

    // then
    await waitFor(() => {
      expect(loading.value).toBe(false);
      expect(error.value.fetchReadiness).not.toBeNull();
    });
  });
});
