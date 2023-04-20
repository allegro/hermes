import { beforeEach, describe, expect } from 'vitest';
import axios from 'axios';
import type { Mocked } from 'vitest';
import { dummyTopicMessagesPreview } from '@/dummy/topic';
import { waitFor } from '@testing-library/vue';
import { useTopicMessagesPreview } from '@/composables/topic/use-topic-messages-preview/useTopicMessagesPreview';

vitest.mock('axios');
const mockedAxios = axios as Mocked<typeof axios>;

describe('useTopicMessagesPreview', () => {
  const topicName = 'pl.allegro.public.group.DummyEvent';

  beforeEach(() => {
    vitest.resetAllMocks();
  });

  it('should fetch topic messages preview', async () => {
    // given
    mockedAxios.get.mockResolvedValueOnce({ data: dummyTopicMessagesPreview });

    // when
    const { messages, error, isLoading } = useTopicMessagesPreview(topicName);

    // then: loading state was indicated
    expect(messages.value).toBeUndefined();
    expect(error.value).toBeFalsy();
    expect(isLoading.value).toBeTruthy();

    // and: endpoints were called
    await waitFor(() => {
      expect(mockedAxios.get.mock.calls[0][0]).toBe(
        `/topics/${topicName}/preview`,
      );
    });

    // and: correct data was returned
    await waitFor(() => {
      expect(messages.value).toEqual(dummyTopicMessagesPreview);
      expect(error.value).toBeFalsy();
      expect(isLoading.value).toBeFalsy();
    });
  });

  it('should set error to true when failed getting topic metrics', async () => {
    // given
    mockedAxios.get.mockRejectedValueOnce({});

    // when
    const { messages, error, isLoading } = useTopicMessagesPreview(topicName);

    // and: correct data was returned
    await waitFor(() => {
      expect(messages.value).toBeUndefined();
      expect(error.value).toBeTruthy();
      expect(isLoading.value).toBeFalsy();
    });
  });
});
