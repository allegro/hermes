import { describe, expect } from 'vitest';
import { dummyTopicMetrics } from '@/dummy/topic';
import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import MetricsList from '@/views/topic/components/metrics-list/MetricsList.vue';

describe('MetricsList', () => {
  it('should render proper heading', () => {
    // given
    const props = { metrics: dummyTopicMetrics };

    // when
    const { getByText } = render(MetricsList, { props });

    // then
    const row = getByText('topicView.metrics.title')!;
    expect(row).toBeVisible();
  });

  it.each([
    { property: 'topicView.metrics.rate', value: '3.4' },
    { property: 'topicView.metrics.deliveryRate', value: '3.5' },
    { property: 'topicView.metrics.published', value: 100 },
    { property: 'topicView.metrics.latency', value: '?' },
    { property: 'topicView.metrics.messageSize', value: '?' },
  ])('should render all metrics properties', ({ property, value }) => {
    // given
    const props = { metrics: dummyTopicMetrics };

    // when
    const { getByText } = render(MetricsList, { props });

    // then
    const row = getByText(property).closest('tr')!;
    expect(row).toBeInTheDocument();
    expect(within(row).getByText(value)).toBeVisible();
  });
});
