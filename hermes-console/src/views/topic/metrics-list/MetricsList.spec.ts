import { createTestingPiniaWithState } from '@/dummy/store';
import { describe, expect } from 'vitest';
import { dummyMetricsDashboardUrl } from '@/dummy/metricsDashboardUrl';
import { dummyTopic, dummyTopicMetrics } from '@/dummy/topic';
import { ref } from 'vue';
import { render } from '@/utils/test-utils';
import { useMetrics } from '@/composables/metrics/use-metrics/useMetrics';
import { within } from '@testing-library/vue';
import MetricsList from '@/views/topic/metrics-list/MetricsList.vue';
import type { UseMetrics } from '@/composables/metrics/use-metrics/useMetrics';

vi.mock('@/composables/metrics/use-metrics/useMetrics');

const useMetricsStub: UseMetrics = {
  dashboardUrl: ref(dummyMetricsDashboardUrl.url),
  loading: ref(false),
  error: ref({
    fetchDashboardUrl: null,
  }),
};

describe('MetricsList', () => {
  it('should render proper heading', () => {
    // given
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    const props = { metrics: dummyTopicMetrics, topicName: dummyTopic.name };

    // when
    const { getByText } = render(MetricsList, {
      props,
      testPinia: createTestingPiniaWithState(),
    });

    // then
    const row = getByText('topicView.metrics.title')!;
    expect(row).toBeVisible();
  });

  it.each([
    { property: 'topicView.metrics.rate', value: '3.40' },
    { property: 'topicView.metrics.deliveryRate', value: '3.50' },
    { property: 'topicView.metrics.published', value: 100 },
  ])('should render all metrics properties %s', ({ property, value }) => {
    // given
    vi.mocked(useMetrics).mockReturnValueOnce(useMetricsStub);
    const props = { metrics: dummyTopicMetrics, topicName: dummyTopic.name };

    // when
    const { getByText } = render(MetricsList, {
      props,
      testPinia: createTestingPiniaWithState(),
    });

    // then
    const row = getByText(property).closest('tr')!;
    expect(row).toBeVisible();
    expect(within(row).getByText(value)).toBeVisible();
  });
});
