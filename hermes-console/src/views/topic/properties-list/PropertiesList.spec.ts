import { Ack } from '@/api/topic';
import { describe, expect } from 'vitest';
import { dummyTopic } from '@/dummy/topic';
import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import PropertiesList from '@/views/topic/properties-list/PropertiesList.vue';
import userEvent from '@testing-library/user-event';

describe('PropertiesList', () => {
  it('should render proper heading', () => {
    // given
    const props = { topic: dummyTopic };

    // when
    const { getByText } = render(PropertiesList, { props });

    // then
    const row = getByText('topicView.properties.title')!;
    expect(row).toBeVisible();
  });

  it.each([
    {
      topic: dummyTopic,
      property: 'topicView.properties.contentType',
      value: 'AVRO',
    },
    {
      topic: dummyTopic,
      property: 'topicView.properties.acknowledgement',
      value: 'topicView.properties.ackText.leader',
    },
    {
      topic: { ...dummyTopic, ack: Ack.ALL },
      property: 'topicView.properties.acknowledgement',
      value: 'topicView.properties.ackText.all',
    },
    {
      topic: { ...dummyTopic, ack: Ack.NONE },
      property: 'topicView.properties.acknowledgement',
      value: 'topicView.properties.ackText.none',
    },
    {
      topic: dummyTopic,
      property: 'topicView.properties.retentionTime',
      value: '1 days',
    },
    {
      topic: dummyTopic,
      property: 'topicView.properties.trackingEnabled',
      value: 'false',
    },
    {
      topic: dummyTopic,
      property: 'topicView.properties.maxMessageSize',
      value: '10240',
    },
    {
      topic: dummyTopic,
      property: 'topicView.properties.authorizationEnabled',
      value: 'false',
    },
    {
      topic: dummyTopic,
      property: 'topicView.properties.authorizedPublishers',
      value: 'topicView.properties.authorizedPublishersNotSet',
    },
    {
      topic: dummyTopic,
      property: 'topicView.properties.allowUnauthenticatedAccess',
      value: 'true',
    },
    {
      topic: dummyTopic,
      property: 'topicView.properties.restrictSubscribing',
      value: 'false',
    },
    {
      topic: dummyTopic,
      property: 'topicView.properties.storeOffline',
      value: 'true',
    },
    {
      topic: dummyTopic,
      property: 'topicView.properties.offlineRetention',
      value: '60 days',
    },
    {
      topic: dummyTopic,
      property: 'topicView.properties.creationDate',
      value: '2021-10-22 15:24:02',
    },
    {
      topic: dummyTopic,
      property: 'topicView.properties.modificationDate',
      value: '2021-11-09 09:45:13',
    },
  ])('should render topic property %s', ({ topic, property, value }) => {
    // given
    const props = { topic: topic };

    // when
    const { getByText } = render(PropertiesList, { props });

    // then
    const row = getByText(property).closest('tr')!;
    expect(row).toBeVisible();
    expect(within(row).getByText(value)).toBeVisible();
  });

  it.each([
    {
      property: 'topicView.properties.acknowledgement',
      tooltip: 'topicView.properties.tooltips.acknowledgement',
    },
    {
      property: 'topicView.properties.retentionTime',
      tooltip: 'topicView.properties.tooltips.retentionTime',
    },
    {
      property: 'topicView.properties.authorizedPublishers',
      tooltip: 'topicView.properties.tooltips.authorizedPublishers',
    },
    {
      property: 'topicView.properties.allowUnauthenticatedAccess',
      tooltip: 'topicView.properties.tooltips.allowUnauthenticatedAccess',
    },
    {
      property: 'topicView.properties.restrictSubscribing',
      tooltip: 'topicView.properties.tooltips.restrictSubscribing',
    },
    {
      property: 'topicView.properties.storeOffline',
      tooltip: 'topicView.properties.tooltips.storeOffline',
    },
    {
      property: 'topicView.properties.offlineRetention',
      tooltip: 'topicView.properties.tooltips.offlineRetention',
    },
  ])('should render property tooltip %s', async ({ property, tooltip }) => {
    // given
    const props = { topic: dummyTopic };

    // when
    const { getByText } = render(PropertiesList, { props });
    const row = getByText(property).closest('tr')!!;
    const tooltipElement = getByText(tooltip);
    await userEvent.hover(tooltipElement);

    // then
    expect(tooltipElement).toBeVisible();
    expect(within(row).getByTestId('tooltip-icon')).toBeVisible();
  });
});
