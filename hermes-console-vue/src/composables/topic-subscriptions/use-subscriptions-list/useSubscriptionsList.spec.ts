import { beforeEach, describe, expect } from 'vitest';
import {
  dummySubscription,
  dummyTopicSubscriptionsList,
  secondDummySubscription,
} from '@/dummy/subscription';
import { useSubscriptionsList } from '@/composables/topic-subscriptions/use-subscriptions-list/useSubscriptionsList';
import { waitFor } from '@testing-library/vue';
import axios from 'axios';
import type { Mocked } from 'vitest';

vitest.mock('axios');
const mockedAxios = axios as Mocked<typeof axios>;

describe('useSubscriptionsList', () => {
  const topicName = 'pl.allegro.public.group.DummyEvent';

  beforeEach(() => {
    vitest.resetAllMocks();
  });

  it('should fetch subscriptions data from API', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({
      data: dummyTopicSubscriptionsList,
    });
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscription });
    mockedAxios.get.mockResolvedValueOnce({ data: secondDummySubscription });

    // when
    const { subscriptions, error, isLoading } = useSubscriptionsList(topicName);

    // then: loading state was indicated
    expect(subscriptions.value).toBeUndefined();
    expect(error.value).toBeFalsy();
    expect(isLoading.value).toBeTruthy();

    // and: endpoints were called
    await waitFor(() => {
      expect(mockedAxios.get.mock.calls[0][0]).toBe(
        `/topics/${topicName}/subscriptions`,
      );
      expect(mockedAxios.get.mock.calls[1][0]).toBe(
        `/topics/${topicName}/subscriptions/foobar-service`,
      );
      expect(mockedAxios.get.mock.calls[2][0]).toBe(
        `/topics/${topicName}/subscriptions/bazbar-service`,
      );
    });

    // and: correct data was returned
    await waitFor(() => {
      expect(subscriptions.value).toEqual([
        dummySubscription,
        secondDummySubscription,
      ]);
      expect(error.value).toBeFalsy();
      expect(isLoading.value).toBeFalsy();
    });
  });

  it("should set error to true when at least one subscription details couldn't be fetched", async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({
      data: dummyTopicSubscriptionsList,
    });
    mockedAxios.get.mockRejectedValueOnce({});
    mockedAxios.get.mockResolvedValueOnce({ data: secondDummySubscription });

    // when
    const { subscriptions, error, isLoading } = useSubscriptionsList(topicName);

    // then
    await waitFor(() => {
      expect(subscriptions.value).toEqual([secondDummySubscription]);
      expect(error.value).toBeTruthy();
      expect(isLoading.value).toBeFalsy();
    });
  });

  it('should set error to true when failed getting subscription list', async () => {
    // given
    mockedAxios.get.mockRejectedValueOnce({});
    mockedAxios.get.mockResolvedValueOnce({ data: dummySubscription });
    mockedAxios.get.mockResolvedValueOnce({ data: secondDummySubscription });

    // when
    const { subscriptions, error, isLoading } = useSubscriptionsList(topicName);

    // then
    await waitFor(() => {
      expect(subscriptions.value).toBeUndefined();
      expect(error.value).toBeTruthy();
      expect(isLoading.value).toBeFalsy();
    });
  });
});
