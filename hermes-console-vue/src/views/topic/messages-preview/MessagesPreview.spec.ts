import { describe, expect } from 'vitest';
import { dummyTopicMessagesPreview } from '@/dummy/topic';
import { render } from '@/utils/test-utils';
import MessagesPreview from '@/views/topic/messages-preview/MessagesPreview.vue';
import userEvent from '@testing-library/user-event';

describe('MessagesPreview', () => {
  const props = { messages: dummyTopicMessagesPreview };

  it('should render title properly', () => {
    // given
    const { getByText } = render(MessagesPreview, { props });

    // expect
    expect(getByText('topicView.messagesPreview.title')).toBeVisible();
  });

  it('should render all messages', async () => {
    // given
    const user = userEvent.setup();
    const { getByText, container } = render(MessagesPreview, { props });

    // when
    await user.click(getByText('topicView.messagesPreview.title'));

    dummyTopicMessagesPreview.forEach((message, index) => {
      const codeBlock = container.querySelectorAll('.v-code')[index];

      // then
      expect(codeBlock).toBeVisible();

      // and
      expect(codeBlock?.textContent?.replace(/\s/g, '')).toEqual(
        message.content,
      );
    });
  });
});
