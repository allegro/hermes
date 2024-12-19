import { afterEach, expect } from 'vitest';
import { createTestingPinia } from '@pinia/testing';
import { dummyConsumerGroups } from '@/dummy/consumerGroups';
import { dummySubscription } from '@/dummy/subscription';
import { dummyTopic } from '@/dummy/topic';
import {
  fetchConsumerGroupsErrorHandler,
  fetchConsumerGroupsHandler,
} from '@/mocks/handlers';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useConsumerGroups } from '@/composables/consumer-groups/use-consumer-groups/useConsumerGroups';
import { waitFor } from '@testing-library/vue';

describe('useConsumerGroups', () => {
  const topicName = dummyTopic.name;
  const subscriptionName = dummySubscription.name;

  const server = setupServer(
    fetchConsumerGroupsHandler({
      consumerGroups: dummyConsumerGroups,
      topicName,
      subscriptionName,
    }),
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

  it('should fetch consumerGroups details from Hermes API', async () => {
    // given
    server.listen();

    // when
    const { consumerGroups, loading, error } = useConsumerGroups(
      topicName,
      subscriptionName,
    );

    // then
    expect(loading.value).toBeTruthy();

    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchConsumerGroups).toBeNull();
      expect(consumerGroups.value).toEqual(consumerGroups.value);
    });
  });

  it('should set error to true on consumerGroups endpoint failure', async () => {
    // given
    server.use(
      fetchConsumerGroupsErrorHandler({
        errorCode: 500,
        topicName,
        subscriptionName,
      }),
    );
    server.listen();

    // when
    const { loading, error } = useConsumerGroups(topicName, subscriptionName);

    // then
    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchConsumerGroups).not.toBeNull();
    });
  });
});
