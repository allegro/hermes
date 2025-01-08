import { describe, expect } from 'vitest';
import { render, renderWithEmits } from '@/utils/test-utils';
import { waitFor } from '@testing-library/vue';
import CreateConstraintFormView from '@/views/admin/constraints/create-constraint-form/CreateConstraintFormView.vue';

describe('CreateConstraintForm', () => {
  it('should emit a cancel event when user clicks cancel button', async () => {
    // given
    const wrapper = renderWithEmits(CreateConstraintFormView, {
      props: {
        isSubscription: false,
      },
    });

    // when
    await wrapper
      .find('[data-testid="createConstraintCancel"]')
      .trigger('click');

    // then
    await waitFor(() => {
      const cancelEvents = wrapper.emitted('cancel')!!;
      expect(cancelEvents).toHaveLength(1);
    });
  });

  it('should render subscription name field when in subscription mode', async () => {
    // when
    const { queryAllByText } = render(CreateConstraintFormView, {
      props: {
        isSubscription: true,
      },
    });

    // then
    expect(
      queryAllByText('constraints.createForm.subscriptionName')[0],
    ).toBeVisible();
  });

  it('should not render subscription name field when in topic mode', async () => {
    // given
    const { queryByText } = render(CreateConstraintFormView, {
      props: {
        isSubscription: false,
      },
    });

    // when
    expect(
      queryByText('constraints.createForm.subscriptionName'),
    ).not.toBeInTheDocument();
  });

  it('should emit a create event when users creates topic constraint', async () => {
    // given
    const wrapper = renderWithEmits(CreateConstraintFormView, {
      props: {
        isSubscription: false,
      },
    });

    // when
    await wrapper
      .findComponent('[data-testid="createConstraintTopicNameInput"]')
      .setValue('pl.allegro.public.group.DummyEvent');
    await wrapper
      .findComponent('[data-testid="createConstraintConsumersNumberInput"]')
      .setValue('10');
    await wrapper
      .findComponent('[data-testid="createConstraintReasonInput"]')
      .setValue('Some test reason');
    await wrapper.find('[data-testid="createConstraintSave"]').trigger('click');

    // then
    await waitFor(() => {
      const createEvents = wrapper.emitted('create')!!;
      expect(createEvents).toHaveLength(1);

      expect(createEvents[0]).toEqual([
        'pl.allegro.public.group.DummyEvent',
        {
          consumersNumber: '10',
          reason: 'Some test reason',
        },
      ]);
    });
  });

  it('should emit a create event when users creates subscription constraint', async () => {
    // given
    const wrapper = renderWithEmits(CreateConstraintFormView, {
      props: {
        isSubscription: true,
      },
    });

    // when
    await wrapper
      .findComponent('[data-testid="createConstraintTopicNameInput"]')
      .setValue('pl.allegro.public.group.DummyEvent');
    await wrapper
      .findComponent('[data-testid="createConstraintSubscriptionNameInput"]')
      .setValue('foobar-service');
    await wrapper
      .findComponent('[data-testid="createConstraintConsumersNumberInput"]')
      .setValue('3');
    await wrapper
      .findComponent('[data-testid="createConstraintReasonInput"]')
      .setValue('Some test reason');
    await wrapper.find('[data-testid="createConstraintSave"]').trigger('click');

    // then
    await waitFor(() => {
      const createEvents = wrapper.emitted('create')!!;
      expect(createEvents).toHaveLength(1);

      expect(createEvents[0]).toEqual([
        'pl.allegro.public.group.DummyEvent$foobar-service',
        {
          consumersNumber: '3',
          reason: 'Some test reason',
        },
      ]);
    });
  });
});
