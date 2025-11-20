import { describe } from 'vitest';
import { dummyOfflineClientsSource } from '@/dummy/topic';
import { render } from '@/utils/test-utils';
import OfflineClients from '@/views/topic/offline-clients/OfflineClients.vue';

describe('OfflineClients', () => {
  const props = { source: dummyOfflineClientsSource.source };

  it('should render offline clients iframe', () => {
    // when
    const { getByTestId } = render(OfflineClients, { props });

    // then
    const component = getByTestId('offline-clients');
    expect(component).toBeVisible();
    expect(component).toHaveAttribute('src', props.source);
  });
});
