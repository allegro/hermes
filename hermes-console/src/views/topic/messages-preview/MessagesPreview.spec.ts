import { describe, expect, it } from 'vitest';
import { dummyTopicMessagesPreview } from '@/dummy/topic';
import { formatTimestampMillis } from '@/utils/date-formatter/date-formatter';
import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import MessagesPreview from '@/views/topic/messages-preview/MessagesPreview.vue';
import userEvent from '@testing-library/user-event';
import type { MessagePreview } from '@/api/topic';

describe('MessagesPreview', () => {
  const props = { messages: dummyTopicMessagesPreview, enabled: true };

  it('should render title properly', () => {
    // given
    const { getByText } = render(MessagesPreview, { props });

    // expect
    expect(getByText('topicView.messagesPreview.title')).toBeVisible();
  });

  it('should render all messages in the table', () => {
    // given
    const { getByText } = render(MessagesPreview, { props });

    // then
    const firstRow = getByText('32fdedf7-a425-4sad-ad85-dd3fec785ccd').closest(
      'tr',
    )!;
    expect(
      within(firstRow).getByText(formatTimestampMillis(Number(1652267893075))),
    ).toBeVisible();
    expect(
      within(firstRow).getByText(dummyTopicMessagesPreview[0].content),
    ).toBeVisible();
    expect(within(firstRow).getByText('false')).toBeVisible();

    // and
    const secondRow = getByText('42fdedf4-a425-4sad-ad85-dd3fec785ccd').closest(
      'tr',
    )!;
    expect(
      within(secondRow).getByText(formatTimestampMillis(Number(1652257893073))),
    ).toBeVisible();
    expect(
      within(secondRow).getByText(dummyTopicMessagesPreview[1].content),
    ).toBeVisible();
    expect(within(secondRow).getByText('false')).toBeVisible();
  });

  it('should render truncated messages in the table', () => {
    // given
    const props = {
      enabled: true,
      messages: [
        {
          content:
            '{"__metadata":{"x-request-id":"65157233-0153-4256-91d6-12d5b60d47a2","messageId":"32fdedf7-a425-4sad-ad85-dd3fec785ccd","trace-sampled":"0","timestamp":"1652267893075"},"waybillId":',
          truncated: true,
        } satisfies MessagePreview,
      ],
    };
    const { getByText, getAllByText } = render(MessagesPreview, { props });

    // then
    expect(
      getAllByText('topicView.messagesPreview.messageDetails.notAvailable')[0],
    ).toBeVisible();
    expect(
      getAllByText('topicView.messagesPreview.messageDetails.notAvailable')[1],
    ).toBeVisible();
    expect(getByText(props.messages[0].content)).toBeVisible();
  });

  it('should open message dialog on row click', async () => {
    // given
    const user = userEvent.setup();
    const { getByText, getByRole } = render(MessagesPreview, { props });

    // when
    await user.click(getByText('32fdedf7-a425-4sad-ad85-dd3fec785ccd'));

    // then
    expect(getByRole('dialog')).toBeVisible();
    expect(
      getByText('topicView.messagesPreview.messageDetails.title'),
    ).toBeVisible();
  });

  it('should open message dialog for truncated message on row click', async () => {
    // given
    const user = userEvent.setup();
    const props = {
      enabled: true,
      messages: [
        {
          content:
            '{"__metadata":{"x-request-id":"65157233-0153-4256-91d6-12d5b60d47a2","messageId":"32fdedf7-a425-4sad-ad85-dd3fec785ccd","trace-sampled":"0","timestamp":"1652267893075"},"waybillId":',
          truncated: true,
        } satisfies MessagePreview,
      ],
    };
    const { getAllByText, getByText, getByRole } = render(MessagesPreview, {
      props,
    });

    // when
    await user.click(
      getAllByText('topicView.messagesPreview.messageDetails.notAvailable')[0],
    );

    // then
    expect(getByRole('dialog')).toBeVisible();
    expect(
      getByText('topicView.messagesPreview.messageDetails.title'),
    ).toBeVisible();
  });

  it('should show appropriate message when props enabled is false', () => {
    // given
    const { getByText } = render(MessagesPreview, {
      props: { ...props, enabled: false },
    });

    // expect
    expect(
      getByText('topicView.messagesPreview.messageDetails.disabled'),
    ).toBeVisible();
  });

  it('should show appropriate message when messages table is empty', () => {
    // given
    const { getByText } = render(MessagesPreview, {
      props: { messages: [], enabled: true },
    });

    // expect
    expect(
      getByText('topicView.messagesPreview.messageDetails.noMessages'),
    ).toBeVisible();
  });

  it('should show appropriate message when enabled is false and messages array is empty', () => {
    // given
    const { getByText } = render(MessagesPreview, {
      props: { messages: [], enabled: false },
    });

    // expect
    expect(
      getByText('topicView.messagesPreview.messageDetails.disabled'),
    ).toBeVisible();
  });
});
