import { appConfigStoreState } from '@/dummy/store';
import { beforeEach, describe, expect, it } from 'vitest';
import { createTestingPinia } from '@pinia/testing';
import { dummyActiveOfflineRetransmissions, dummyTopic } from '@/dummy/topic';
import { dummyAppConfig } from '@/dummy/app-config';
import { dummyRoles } from '@/dummy/roles';
import { render } from '@/utils/test-utils';
import { setActivePinia } from 'pinia';
import OfflineRetransmissionInfo from '@/views/topic/offline-retransmission/OfflineRetransmissionInfo.vue';
import userEvent from '@testing-library/user-event';

describe('OfflineRetransmissionInfo', () => {
  const pinia = createTestingPinia({
    fakeApp: true,
    initialState: {
      appConfig: {
        ...appConfigStoreState,
        appConfig: {
          ...dummyAppConfig,
          topic: {
            ...dummyAppConfig.topic,
            offlineRetransmission: {
              enabled: true,
              globalTaskQueueUrl: 'https://mocked-url.com',
              monitoringDocsUrl: 'https://docs-url.com',
              fromViewDocsUrl: 'https://docs-url.com',
            },
          },
        },
      },
    },
  });

  const props = {
    tasks: dummyActiveOfflineRetransmissions,
    topic: dummyTopic,
    roles: dummyRoles,
  };

  beforeEach(async () => {
    setActivePinia(pinia);
  });

  it('should render the correct number of rows', async () => {
    // given
    const { getByText } = render(OfflineRetransmissionInfo, {
      props,
      testPinia: pinia,
    });

    // when
    const title = getByText('offlineRetransmission.monitoringView.title');
    const subtitle = getByText(
      `${dummyActiveOfflineRetransmissions.length} offlineRetransmission.monitoringView.activeTasks`,
    );

    // then
    expect(title).toBeVisible();
    expect(subtitle).toBeVisible();

    // then
    dummyActiveOfflineRetransmissions.forEach((task) => {
      expect(getByText(task.taskId)).toBeVisible();
    });
  });

  it('should render the link to the global task queue', async () => {
    // given
    const { getByText } = render(OfflineRetransmissionInfo, {
      props,
      testPinia: pinia,
    });

    // when
    const link = getByText(
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
    const { getByText } = render(OfflineRetransmissionInfo, {
      props,
      testPinia: pinia,
    });

    // when
    const link = getByText(
      'offlineRetransmission.monitoringView.monitoringDocsLinkTitle',
    );

    // then
    expect(link).toBeVisible();
    expect(link.closest('a')?.getAttribute('href')).toBe(
      'https://docs-url.com',
    );
  });

  it('should open new retransmission task dialog on button click', async () => {
    // given
    const user = userEvent.setup();
    const { getByText } = render(OfflineRetransmissionInfo, {
      props,
      testPinia: pinia,
    });

    // when
    const newRetransmissionButton = getByText(
      'offlineRetransmission.monitoringView.newRetransmissionTask',
    );
    await user.click(newRetransmissionButton);

    // then
    const dialogTitle = getByText('offlineRetransmission.title');
    expect(dialogTitle).toBeVisible();
  });

  it.each([
    {
      offlineRetransmission: {
        enabled: false,
      },
      offlineStorageEnabled: false,
      show: false,
    },
    {
      offlineRetransmission: {
        enabled: true,
      },
      offlineStorageEnabled: false,
      show: false,
    },
    {
      offlineRetransmission: {
        enabled: false,
      },
      offlineStorageEnabled: true,
      show: false,
    },
    {
      offlineRetransmission: {
        enabled: true,
      },
      offlineStorageEnabled: true,
      show: true,
    },
  ])(
    'should show or hide new offline retransmission task button',
    ({ offlineRetransmission, offlineStorageEnabled, show }) => {
      // when
      const testPinia = createTestingPinia({
        initialState: {
          appConfig: {
            ...appConfigStoreState,
            appConfig: {
              ...dummyAppConfig,
              topic: { ...dummyAppConfig.topic, offlineRetransmission },
            },
          },
        },
      });
      const props = {
        topic: {
          ...dummyTopic,
          offlineStorage: {
            enabled: offlineStorageEnabled,
            retentionTime: {
              duration: 60,
              infinite: false,
            },
          },
        },
        roles: dummyRoles,
        tasks: [],
      };
      const { queryByText } = render(OfflineRetransmissionInfo, {
        props,
        testPinia,
      });

      // then
      if (show) {
        expect(
          queryByText(
            'offlineRetransmission.monitoringView.newRetransmissionTask',
          ),
        ).toBeVisible();
      } else {
        expect(
          queryByText(
            'offlineRetransmission.monitoringView.newRetransmissionTask',
          ),
        ).not.toBeInTheDocument();
      }
    },
  );

  it('should display tasks correctly in the table', () => {
    // given
    const { getByText, getAllByText } = render(OfflineRetransmissionInfo, {
      props,
      testPinia: pinia,
    });

    // then
    dummyActiveOfflineRetransmissions.forEach((task) => {
      expect(getByText(task.taskId)).toBeVisible();
      expect(getByText(task.type)).toBeVisible();
      expect(
        getAllByText('offlineRetransmission.monitoringView.logsLinkTitle')[0],
      ).toBeVisible();
      expect(
        getAllByText(
          'offlineRetransmission.monitoringView.metricsLinkTitle',
        )[0],
      ).toBeVisible();
      expect(
        getAllByText(
          'offlineRetransmission.monitoringView.detailsLinkTitle',
        )[0],
      ).toBeVisible();
    });
  });
});
