import { describe, expect } from 'vitest';
import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import { dummyTopic } from '@/dummy/topic';
import PropertiesList from '@/views/topic/components/properties-list/PropertiesList.vue';
import { Ack } from '@/api/topic';

describe('PropertiesList', () => {
  it('should render proper heading', () => {
    // given
    const props = { topic: dummyTopic };

    // when
    const { getByText } = render(PropertiesList, { props });

    // then
    const row = getByText('topicView.properties.header')!;
    expect(row).toBeInTheDocument();
  });

  it.each([
    {
      topic: dummyTopic,
      property: 'topicView.properties.contentType',
      value: 'AVRO',
    },
    {
      topic: dummyTopic,
      property: 'topicView.properties.labels',
      value: 'internal, analytics',
    },
    {
      topic: { ...dummyTopic, labels: [{ value: 'internal' }] },
      property: 'topicView.properties.labels',
      value: 'internal',
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
      property: 'topicView.properties.schemaIdAwareSerializationEnabled',
      value: 'false',
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
  ])('should render topic property', ({ topic, property, value }) => {
    // given
    const props = { topic: topic };

    // when
    const { getByText } = render(PropertiesList, { props });

    // then
    const row = getByText(property).closest('tr')!;
    expect(row).toBeInTheDocument();
    expect(within(row).getByText(value)).toBeInTheDocument();
  });

  it.each([
    {
      property: 'topicView.properties.acknowledgement',
      tooltip: 'topicView.properties.tooltips.acknowledgement',
    },
  ])('should render property tooltip', ({ property, tooltip }) => {
    // given
    const props = { topic: dummyTopic };

    // when
    const { getByText } = render(PropertiesList, { props });

    // then
    const row = getByText(property);
    expect(row).toBeInTheDocument();
    expect(within(row).getByText(tooltip)).toBeInTheDocument();
  });
});
