import { beforeEach, describe, expect, it } from 'vitest';
import { createTestingPinia } from '@pinia/testing';
import { dummyActiveOfflineRetransmissions } from '@/dummy/topic';
import { render } from '@/utils/test-utils';
import { setActivePinia } from 'pinia';
import OfflineRetransmissionInfo from '@/views/topic/offline-retransmission/OfflineRetransmissionInfo.vue';
import userEvent from '@testing-library/user-event';

vi.mock('@/store/app-config/useAppConfigStore', () => ({
  useAppConfigStore: vi.fn(() => ({
    loadedConfig: {
      topic: {
        offlineRetransmissionGlobalTaskQueueUrl: 'https://mocked-url.com',
        offlineRetransmissionMonitoringDocsUrl: 'https://docs-url.com',
      },
    },
  })),
}));

describe('OfflineRetransmissionInfo', () => {
  const pinia = createTestingPinia({
    fakeApp: true,
  });

  const props = {
    tasks: dummyActiveOfflineRetransmissions,
  };

  beforeEach(async () => {
    setActivePinia(pinia);
  });

  it('should render the correct number of rows', async () => {
    // given
    const wrapper = render(OfflineRetransmissionInfo, { props });

    // when
    const table = wrapper.getByText(
      'offlineRetransmission.monitoringView.title (2)',
    );

    // then
    expect(table).toBeVisible();

    // when
    await userEvent.click(table);
    dummyActiveOfflineRetransmissions.forEach((task) => {
      expect(wrapper.getByText(task.taskId)).toBeVisible();
    });
  });

  it('should render the link to the global task queue', async () => {
    // given
    const wrapper = render(OfflineRetransmissionInfo, { props });
    const table = wrapper.getByText(
      'offlineRetransmission.monitoringView.title (2)',
    );
    await userEvent.click(table);

    // when
    const link = wrapper.getByText(
      'offlineRetransmission.monitoringView.allTasksLinkTitle',
    );

    // then
    expect(link).toBeVisible();
    expect(link.closest('a')?.getAttribute('href')).toBe(
      'https://mocked-url.com',
    );
  });

  it('should render the link to the documentation', async () => {
    // given
    const wrapper = render(OfflineRetransmissionInfo, { props });
    const table = wrapper.getByText(
      'offlineRetransmission.monitoringView.title (2)',
    );
    await userEvent.click(table);

    // when
    const link = wrapper.getByText(
      'offlineRetransmission.monitoringView.monitoringDocsLinkTitle',
    );

    // then
    expect(link).toBeVisible();
    expect(link.closest('a')?.getAttribute('href')).toBe(
      'https://docs-url.com',
    );
  });
});
