import { describe, expect, it } from 'vitest';
import { render, renderWithEmits } from '@/utils/test-utils';
import MessagePreviewDialog from '@/views/topic/messages-preview/message-preview-dialog/MessagePreviewDialog.vue';
import { dummyTopicMessagesPreview } from '@/dummy/topic';
import type { ParsedMessagePreview } from '@/views/topic/messages-preview/types';
import { formatTimestampMillis } from '@/utils/date-formatter/date-formatter';

const messageWithParsedContent: ParsedMessagePreview = {
  ...dummyTopicMessagesPreview[0],
  messageId: '32fdedf7-a425-4sad-ad85-dd3fec785ccd',
  timestamp: 1652257893073,
  parsedContent: JSON.parse(dummyTopicMessagesPreview[0].content),
};

const messageWithoutParsedContent: ParsedMessagePreview = {
  ...dummyTopicMessagesPreview[0],
  content: 'not a json',
  messageId: null,
  timestamp: null,
  parsedContent: null,
};

describe('MessagePreviewDialog', () => {
  it('should render title and subtitle', () => {
    // given
    const { getByText } = render(MessagePreviewDialog, {
      props: { message: messageWithParsedContent },
    });

    // then
    expect(
      getByText('topicView.messagesPreview.messageDetails.title'),
    ).toBeVisible();
    expect(
      getByText('topicView.messagesPreview.messageDetails.subtitle'),
    ).toBeVisible();
  });

  it('should emit close event on close button click', async () => {
    // given
    const wrapper = renderWithEmits(MessagePreviewDialog, {
      props: { message: messageWithParsedContent },
    });

    // when
    await wrapper.find('[data-testid="close-button"]').trigger('click');

    // then
    expect(wrapper.emitted()).toHaveProperty('close');
  });

  it('should render message details', () => {
    // given
    const { getByText } = render(MessagePreviewDialog, {
      props: { message: messageWithParsedContent },
    });

    // then
    expect(
      getByText(messageWithParsedContent.messageId as string),
    ).toBeVisible();
    expect(
      getByText(
        formatTimestampMillis(messageWithParsedContent.timestamp as number),
      ),
    ).toBeVisible();
  });

  it('should render "not available" when messageId or timestamp is not available', () => {
    // given
    const { getAllByText } = render(MessagePreviewDialog, {
      props: { message: messageWithoutParsedContent },
    });

    // then
    expect(
      getAllByText('topicView.messagesPreview.messageDetails.notAvailable')
        .length,
    ).toBe(2);
  });

  it('should render JsonViewer when content is parsable', () => {
    // given
    const { getByTestId } = render(MessagePreviewDialog, {
      props: { message: messageWithParsedContent },
    });

    // then
    expect(getByTestId('json-viewer')).toBeVisible();
  });

  it('should render raw content when content is not parsable', () => {
    // given
    const { getByText } = render(MessagePreviewDialog, {
      props: { message: messageWithoutParsedContent },
    });

    // then
    expect(getByText(messageWithoutParsedContent.content)).toBeVisible();
  });
});
