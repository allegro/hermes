import { afterEach, describe } from 'vitest';
import {
  dummySubscription,
  secondDummySubscription,
} from '@/dummy/subscription';
import { dummyTopic } from '@/dummy/topic';
import {
  querySubscriptionsErrorHandler,
  querySubscriptionsHandler,
  queryTopicsErrorHandler,
  queryTopicsHandler,
} from '@/mocks/handlers';
import { SearchFilter, useSearch } from '@/composables/search/useSearch';
import { setupServer } from 'msw/node';
import { waitFor } from '@testing-library/vue';

describe('useSearch', () => {
  const server = setupServer(queryTopicsHandler({ topics: [dummyTopic] }));

  afterEach(() => {
    server.resetHandlers();
  });

  it('should retrieve topic query results from Hermes API', async () => {
    // given
    server.listen();

    // when
    const { topics, queryTopicsFn, loading, error } = useSearch();

    queryTopicsFn(SearchFilter.NAME, 'query');

    // then
    expect(loading.value).toBeTruthy();

    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchError).toBeNull();
      expect(topics.value).toEqual([dummyTopic]);
    });
  });

  it('should retrieve subscription query results from Hermes API', async () => {
    // given
    server.use(
      querySubscriptionsHandler({
        subscriptions: [dummySubscription, secondDummySubscription],
      }),
    );
    server.listen();

    // when
    const { subscriptions, querySubscriptionsFn, loading, error } = useSearch();

    querySubscriptionsFn(SearchFilter.NAME, 'query');

    // then
    expect(loading.value).toBeTruthy();

    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchError).toBeNull();
      expect(subscriptions.value).toEqual([
        dummySubscription,
        secondDummySubscription,
      ]);
    });
  });

  it('should set error to true on query topics endpoint failure', async () => {
    // given
    server.use(queryTopicsErrorHandler({ errorCode: 500 }));
    server.listen();

    // when
    const { queryTopicsFn, loading, error } = useSearch();
    queryTopicsFn(SearchFilter.ENDPOINT, 'query');

    // then
    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchError).not.toBeNull();
    });
  });

  it('should set error to true on query subscriptions endpoint failure', async () => {
    // given
    server.use(querySubscriptionsErrorHandler({ errorCode: 500 }));
    server.listen();

    // when
    const { querySubscriptionsFn, loading, error } = useSearch();
    querySubscriptionsFn(SearchFilter.ENDPOINT, 'query');

    // then
    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchError).not.toBeNull();
    });
  });
});
