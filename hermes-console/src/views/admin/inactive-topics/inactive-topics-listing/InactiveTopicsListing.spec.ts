import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import InactiveTopicsListing from '@/views/admin/inactive-topics/inactive-topics-listing/InactiveTopicsListing.vue';

describe('InactiveTopicsListing', () => {
  it('should render inactive topics listing', () => {
    // given
    const testInactiveTopics = [
      {
        topic: 'group.topic1',
        lastPublishedTsMs: 1732499845200, // 2024-11-25
        notificationTsMs: [
          1733499835210, // 2024-12-06
          1734445345212, // 2024-12-17
        ],
        whitelisted: false,
      },
      {
        topic: 'group.topic2',
        lastPublishedTsMs: 1633928665148, // 2021-10-11
        notificationTsMs: [],
        whitelisted: true,
      },
    ];

    const props = {
      inactiveTopics: testInactiveTopics,
    };

    // when
    const { getByText } = render(InactiveTopicsListing, { props });

    // then
    const topic1Row = getByText('group.topic1')
      .closest('tr')!
      .querySelectorAll('td');
    expect(within(topic1Row[0]).getByText('group.topic1')).toBeVisible();
    expect(within(topic1Row[1]).getByText('2024-11-25')).toBeVisible();
    expect(within(topic1Row[2]).getByText('2024-12-17')).toBeVisible();
    expect(within(topic1Row[3]).getByText('2')).toBeVisible();
    expect(within(topic1Row[4]).getByText('false')).toBeVisible();

    // and
    const topic2Row = getByText('group.topic2')
      .closest('tr')!
      .querySelectorAll('td');
    expect(within(topic2Row[0]).getByText('group.topic2')).toBeVisible();
    expect(within(topic2Row[1]).getByText('2021-10-11')).toBeVisible();
    expect(topic2Row[2].textContent).toBe('');
    expect(within(topic2Row[3]).getByText('0')).toBeVisible();
    expect(within(topic2Row[4]).getByText('true')).toBeVisible();
  });

  it('should render empty inactive topics listing', () => {
    // given
    const props = {
      inactiveTopics: [],
    };

    // when
    const { getByText } = render(InactiveTopicsListing, { props });

    // then
    expect(getByText('No data available')).toBeVisible();
  });
});
