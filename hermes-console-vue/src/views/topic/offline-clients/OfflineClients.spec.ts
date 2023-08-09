import { describe } from 'vitest';
import { dummyOfflineClientsSource } from '@/dummy/topic';
import { render } from '@/utils/test-utils';
import OfflineClients from '@/views/topic/offline-clients/OfflineClients.vue';
// import userEvent from '@testing-library/user-event';

describe('OfflineClients', () => {
  const props = { source: dummyOfflineClientsSource.source };

  it('should render proper heading', () => {
    // when
    const { getByText } = render(OfflineClients, { props });

    // then
    const row = getByText('topicView.offlineClients.title');
    expect(row).toBeVisible();
  });

  // it('should render iframe when section is opened', async () => {
  //   // given
  //   const { getByText, container } = render(OfflineClients, { props });
  //
  //   // when
  //   await userEvent.click(getByText('topicView.offlineClients.title'));
  //   const element = container.querySelector('iframe');
  //
  //   // then
  // });
});
