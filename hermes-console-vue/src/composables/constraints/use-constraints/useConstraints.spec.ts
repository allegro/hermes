import { afterEach } from 'vitest';
import { dummyConstraints } from '@/dummy/constraints';
import {
  fetchConstraintsErrorHandler,
  fetchConstraintsHandler,
} from '@/mocks/handlers';
import { setupServer } from 'msw/node';
import { useConstraints } from '@/composables/constraints/use-constraints/useConstraints';
import { waitFor } from '@testing-library/vue';

describe('useConstraints', () => {
  const server = setupServer(
    fetchConstraintsHandler({ constraints: dummyConstraints }),
  );

  afterEach(() => {
    server.resetHandlers();
  });

  it('should fetch constraints names from Hermes backend', async () => {
    // given
    server.listen();

    // when
    const { topicConstraints, subscriptionConstraints, loading, error } =
      useConstraints();

    // then
    expect(loading.value).toBeTruthy();
    expect(error.value.fetchConstraints).toBeNull();

    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchConstraints).toBeNull();
      expect(topicConstraints.value?.['pl.group.Topic1'].consumersNumber).toBe(
        2,
      );
      expect(
        subscriptionConstraints.value?.['pl.group.Topic$subscription2']
          .consumersNumber,
      ).toBe(8);
    });
  });

  it('should set error to true on workload endpoint failure', async () => {
    // given
    server.use(fetchConstraintsErrorHandler({ errorCode: 500 }));
    server.listen();

    // when
    const { error } = useConstraints();

    // then
    await waitFor(() => {
      expect(error.value.fetchConstraints).not.toBeNull();
    });
  });
});
