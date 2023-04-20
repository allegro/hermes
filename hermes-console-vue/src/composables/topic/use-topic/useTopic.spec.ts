import { beforeEach, describe, expect } from 'vitest';
import axios from 'axios';
import type { Mocked } from 'vitest';
import { dummyTopic, dummyTopicOwner } from '@/dummy/topic';
import { waitFor } from '@testing-library/vue';
import { useTopic } from '@/composables/topic/use-topic/useTopic';

vitest.mock('axios');
const mockedAxios = axios as Mocked<typeof axios>;

describe('useTopic', () => {
  const topicName = 'pl.allegro.public.group.DummyEvent';
  const topicOwnerId = '41';

  beforeEach(() => {
    vitest.resetAllMocks();
  });

  it('should fetch topic', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummyTopic });
    mockedAxios.get.mockResolvedValueOnce({ data: dummyTopicOwner });

    // when
    const {
      topic,
      owner,
      topicError,
      ownerError,
      topicIsLoading,
      ownerIsLoading,
    } = useTopic(topicName);

    // then: loading state was indicated
    expect(topic.value).toBeUndefined();
    expect(owner.value).toBeUndefined();
    expect(topicError.value).toBeFalsy();

    expect(ownerError.value).toBeFalsy();
    expect(topicIsLoading.value).toBeTruthy();
    expect(ownerIsLoading.value).toBeTruthy();

    // and: endpoints were called
    await waitFor(() => {
      expect(mockedAxios.get.mock.calls[0][0]).toBe(`/topics/${topicName}`);
      expect(mockedAxios.get.mock.calls[1][0]).toBe(
        `/owners/sources/Service Catalog/${topicOwnerId}`,
      );
    });

    // and: correct data was returned
    await waitFor(() => {
      expect(topic.value).toEqual(dummyTopic);
      expect(topicError.value).toBeFalsy();
      expect(topicIsLoading.value).toBeFalsy();

      expect(owner.value).toEqual(dummyTopicOwner);
      expect(ownerError.value).toBeFalsy();
      expect(ownerIsLoading.value).toBeFalsy();
    });
  });

  it('should indicate errors when fetching topic failed', async () => {
    // given
    mockedAxios.get.mockRejectedValueOnce({});
    mockedAxios.get.mockResolvedValueOnce({ data: dummyTopicOwner });

    // when
    const {
      topic,
      owner,
      topicError,
      ownerError,
      topicIsLoading,
      ownerIsLoading,
    } = useTopic(topicName);

    // then
    await waitFor(() => {
      expect(topic.value).toBeUndefined();
      expect(topicError.value).toBeTruthy();
      expect(topicIsLoading.value).toBeFalsy();

      expect(owner.value).toBeUndefined();
      expect(ownerError.value).toBeTruthy();
      expect(ownerIsLoading.value).toBeFalsy();
    });
  });

  it('should indicate error when failed getting topic owner', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummyTopic });
    mockedAxios.get.mockRejectedValueOnce({});

    // when
    const {
      topic,
      owner,
      topicError,
      ownerError,
      topicIsLoading,
      ownerIsLoading,
    } = useTopic(topicName);

    // then
    await waitFor(() => {
      expect(topic.value).toEqual(dummyTopic);
      expect(topicError.value).toBeFalsy();
      expect(topicIsLoading.value).toBeFalsy();

      expect(owner.value).toBeUndefined();
      expect(ownerError.value).toBeTruthy();
      expect(ownerIsLoading.value).toBeFalsy();
    });
  });
});
