import { describe, expect } from 'vitest';
import { renderWithEmits } from '@/utils/test-utils';
import { waitFor } from '@testing-library/vue';
import EditConstraintFormView from '@/views/admin/constraints/edit-constraint-form/EditConstraintFormView.vue';

describe('EditConstraintForm', () => {
  it('should emit a cancel event when user clicks cancel button', async () => {
    // given
    const wrapper = renderWithEmits(EditConstraintFormView, {
      props: {
        resourceId: 'resource1',
        constraint: { consumersNumber: 5 },
      },
    });

    // when
    await wrapper.find('[data-testid="editConstraintCancel"]').trigger('click');

    // then
    await waitFor(() => {
      const cancelEvents = wrapper.emitted('cancel')!!;
      expect(cancelEvents).toHaveLength(1);
    });
  });

  it('should emit a delete event when users clicks remove button', async () => {
    // given
    const wrapper = renderWithEmits(EditConstraintFormView, {
      props: {
        resourceId: 'resource1',
        constraint: { consumersNumber: 5 },
      },
    });

    // when
    await wrapper.find('[data-testid="editConstraintRemove"]').trigger('click');

    // then
    await waitFor(() => {
      const deleteEvents = wrapper.emitted('delete')!!;
      expect(deleteEvents).toHaveLength(1);
      expect(deleteEvents[0]).toEqual(['resource1']);
    });
  });

  it('should emit a update event when users updated consumer number and clicks save', async () => {
    // given
    const wrapper = renderWithEmits(EditConstraintFormView, {
      props: {
        resourceId: 'resource1',
        constraint: { consumersNumber: 5 },
      },
    });

    // when
    await wrapper
      .findComponent('[data-testid="editConstraintConsumersNumberInput"]')
      .setValue('10');
    await wrapper.find('[data-testid="editConstraintSave"]').trigger('click');

    // then
    await waitFor(() => {
      const updateEvents = wrapper.emitted('update')!!;
      expect(updateEvents).toHaveLength(1);

      expect(updateEvents[0]).toEqual([
        'resource1',
        {
          consumersNumber: '10',
        },
      ]);
    });
  });
});
