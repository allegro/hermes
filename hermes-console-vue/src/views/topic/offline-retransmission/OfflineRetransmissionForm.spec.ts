import { describe } from 'vitest';
import { renderWithEmits } from '@/utils/test-utils';
import { waitFor } from '@testing-library/vue';
import OfflineRetransmissionForm from '@/views/topic/offline-retransmission/OfflineRetransmissionForm.vue';

describe('OfflineRetransmissionForm', () => {
  it('should emit a cancel event when user clicks cancel button', async () => {
    // given
    const wrapper = renderWithEmits(OfflineRetransmissionForm, {});

    // when
    await wrapper
      .find('[data-testid="offlineRetransmissionCancel"]')
      .trigger('click');

    // then
    await waitFor(() => {
      const cancelEvents = wrapper.emitted('cancel')!!;
      expect(cancelEvents).toHaveLength(1);
    });
  });

  it('should emit a retransmit event when user clicks retransmit button', async () => {
    // given
    const wrapper = renderWithEmits(OfflineRetransmissionForm, {});

    // when
    await wrapper
      .findComponent(
        '[data-testid="offlineRetransmissionTargetTopicNameInput"]',
      )
      .setValue('target.topic');

    await wrapper
      .findComponent('[data-testid="offlineRetransmissionStartTimestampInput"]')
      .setValue('2023-11-10T10:00:00');

    await wrapper
      .findComponent('[data-testid="offlineRetransmissionEndTimestampInput"]')
      .setValue('2023-11-19T10:00:00');

    await wrapper
      .find('[data-testid="offlineRetransmissionRetransmit"]')
      .trigger('click');

    // then
    await waitFor(() => {
      const retransmitEvents = wrapper.emitted('retransmit')!!;
      expect(retransmitEvents).toHaveLength(1);
      expect(retransmitEvents[0]).toEqual([
        'target.topic',
        '2023-11-10T10:00:00',
        '2023-11-19T10:00:00',
      ]);
    });
  });
});
