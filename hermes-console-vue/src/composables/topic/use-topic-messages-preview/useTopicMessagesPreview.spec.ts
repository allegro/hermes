import { beforeEach, describe, expect } from 'vitest';
import { dummyTopicMessagesPreview } from '@/dummy/topic';
import { useTopicMessagesPreview } from '@/composables/topic/use-topic-messages-preview/useTopicMessagesPreview';
import { waitFor } from '@testing-library/vue';
import axios from 'axios';
import type { Mocked } from 'vitest';

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
    const { data, error, isLoading } = useTopicMessagesPreview(topicName);

    // then
    await waitFor(() => {
      expect(data.value).toEqual(dummyTopicMessagesPreview);
      expect(error.value).toBeFalsy();
      expect(isLoading.value).toBeFalsy();
    });
  });
});
