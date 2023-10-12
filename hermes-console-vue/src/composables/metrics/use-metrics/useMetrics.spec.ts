import { afterEach } from 'vitest';
import { createTestingPinia } from '@pinia/testing';
import { dummyMetricsDashboardUrl } from '@/dummy/metricsDashboardUrl';
import { dummySubscription } from '@/dummy/subscription';
import { dummyTopic } from '@/dummy/topic';
import {
  expectNotificationDispatched,
  notificationStoreSpy,
} from '@/utils/test-utils';
import {
  fetchMetricsDashboardUrlErrorHandler,
  fetchMetricsDashboardUrlHandler,
} from '@/mocks/handlers';
import { setActivePinia } from 'pinia';
import { setupServer } from 'msw/node';
import { useMetrics } from '@/composables/metrics/use-metrics/useMetrics';
import { waitFor } from '@testing-library/vue';

describe('useMetrics', () => {
  const server = setupServer();

  const pinia = createTestingPinia({
    fakeApp: true,
  });

  beforeEach(() => {
    setActivePinia(pinia);
  });

  afterEach(() => {
    server.resetHandlers();
  });

  it('should fetch dashboard url from Hermes API for topic', async () => {
    // given
    server.use(
      fetchMetricsDashboardUrlHandler({
        dashboardUrl: dummyMetricsDashboardUrl,
        path: `/dashboards/topics/${dummyTopic.name}`,
      }),
    );
    server.listen();

    // when
    const { dashboardUrl, loading, error } = useMetrics(dummyTopic.name, null);

    // then
    expect(loading.value).toBeTruthy();

    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchDashboardUrl).toBeNull();
      expect(dashboardUrl.value).toEqual(dummyMetricsDashboardUrl.url);
    });
  });

  it('should fetch dashboard url from Hermes API for subscription', async () => {
    // given
    server.use(
      fetchMetricsDashboardUrlHandler({
        dashboardUrl: dummyMetricsDashboardUrl,
        path: `/dashboards/topics/${dummySubscription.topicName}/subscriptions/${dummySubscription.name}`,
      }),
    );
    server.listen();

    // when
    const { dashboardUrl, loading, error } = useMetrics(
      dummySubscription.topicName,
      dummySubscription.name,
    );

    // then
    expect(loading.value).toBeTruthy();

    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchDashboardUrl).toBeNull();
      expect(dashboardUrl.value).toEqual(dummyMetricsDashboardUrl.url);
    });
  });

  it('should set error to true on dashboards endpoint failure', async () => {
    // given
    server.use(
      fetchMetricsDashboardUrlErrorHandler({
        errorCode: 500,
        path: `/dashboards/topics/${dummyTopic.name}`,
      }),
    );
    server.listen();
    const notificationStore = notificationStoreSpy();

    // when
    const { loading, error } = useMetrics(dummyTopic.name, null);

    // then
    await waitFor(() => {
      expect(loading.value).toBeFalsy();
      expect(error.value.fetchDashboardUrl).not.toBeNull();
      expectNotificationDispatched(notificationStore, {
        type: 'warning',
        text: 'notifications.dashboardUrl.error',
      });
    });
  });
});
