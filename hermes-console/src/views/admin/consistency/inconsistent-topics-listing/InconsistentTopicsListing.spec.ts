import { dummyInconsistentTopics } from '@/dummy/inconsistentTopics';
import { render, renderWithEmits } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import InconsistentTopicsListing from '@/views/admin/consistency/inconsistent-topics-listing/InconsistentTopicsListing.vue';

describe('ConstraintsListing', () => {
  it('should render inconsistent topics listing with no filter applied', () => {
    // given
    const props = {
      inconsistentTopics: dummyInconsistentTopics,
    };

    // when
    const { getByText } = render(InconsistentTopicsListing, { props });

    // then
    dummyInconsistentTopics.forEach((topic, index) => {
      const inconsistentTopic = getByText(topic).closest('tr')!;
      expect(within(inconsistentTopic).getByText(`${index + 1}`)).toBeVisible();
      expect(
        within(inconsistentTopic).getByText(
          'consistency.inconsistentTopics.actions.delete',
        ),
      ).toBeVisible();
    });
  });

  it.each(['dummy', 'Dummy', 'DUMMY'])(
    'should render inconsistent topics listing with a filter applied (case-insensitive, filter: %s)',
    (filter: string) => {
      // given
      const props = {
        inconsistentTopics: dummyInconsistentTopics,
        filter,
      };

      // when
      const inconsistentTopic = render(InconsistentTopicsListing, { props })
        .getAllByText(/Dummy/)
        .map((inconsistentTopic) => inconsistentTopic.closest('tr'));

      // then
      expect(inconsistentTopic).toHaveLength(1);
      expect(
        within(inconsistentTopic[0]!).getByText(
          'consistency.inconsistentTopics.actions.delete',
        ),
      ).toBeVisible();
    },
  );

  it('should render inconsistent topics listing with a filter applied (no results)', () => {
    // given
    const props = {
      inconsistentTopics: dummyInconsistentTopics,
      filter: 'pl.group.NoExistingTopic',
    };

    // when
    const rows = render(InconsistentTopicsListing, { props }).getAllByRole(
      'row',
    );

    // then
    expect(rows).toHaveLength(2);
    expect(
      within(rows[0]!).getByText(
        'consistency.inconsistentTopics.listing.index',
      ),
    ).toBeVisible();
    expect(
      within(rows[0]!).getByText('consistency.inconsistentTopics.listing.name'),
    ).toBeVisible();
    expect(
      within(rows[1]!).getByText(/consistency.inconsistentTopics.noTopics/),
    ).toBeVisible();
    expect(
      within(rows[1]!).getByText(
        /consistency.inconsistentTopics.appliedFilter/,
      ),
    ).toBeVisible();
  });

  it('should render an empty inconsistent topics listing without filter applied', () => {
    // given
    const props = {
      inconsistentTopics: [],
    };

    // when
    const rows = render(InconsistentTopicsListing, { props }).getAllByRole(
      'row',
    );

    // then
    expect(rows).toHaveLength(2);
    expect(
      within(rows[1]!).getByText('consistency.inconsistentTopics.noTopics'),
    ).toBeVisible();
    expect(
      within(rows[1]!).queryByText(
        'consistency.inconsistentTopics.appliedFilter',
      ),
    ).not.toBeInTheDocument();
  });

  it('should select all topics visible under the active filter', async () => {
    // given
    const wrapper = renderWithEmits(InconsistentTopicsListing, {
      props: {
        inconsistentTopics: dummyInconsistentTopics,
        filter: 'Topic',
        selectedTopics: [],
      },
    });

    // when
    await wrapper
      .find('[data-testid="select-all-inconsistent-topics"] input')
      .setValue(true);

    // then
    expect(wrapper.emitted('update:selectedTopics')).toEqual([
      [['pl.allegro.group.Topic1_avro', 'pl.allegro.group.Topic2_avro']],
    ]);
  });

  it('should allow opting out a selected topic', async () => {
    // given
    const wrapper = renderWithEmits(InconsistentTopicsListing, {
      props: {
        inconsistentTopics: dummyInconsistentTopics,
        selectedTopics: dummyInconsistentTopics,
      },
    });

    // when
    await wrapper
      .find(`input[aria-label="${dummyInconsistentTopics[2]}"]`)
      .setValue(false);

    // then
    expect(wrapper.emitted('update:selectedTopics')).toEqual([
      [[dummyInconsistentTopics[0], dummyInconsistentTopics[1]]],
    ]);
  });

  it('should disable selection and removal while a batch is running', () => {
    // given
    const wrapper = renderWithEmits(InconsistentTopicsListing, {
      props: {
        inconsistentTopics: dummyInconsistentTopics,
        selectedTopics: dummyInconsistentTopics,
        disabled: true,
      },
    });

    // then
    wrapper.findAll('input[type="checkbox"]').forEach((checkbox) => {
      expect(checkbox.attributes('disabled')).toBeDefined();
    });
    wrapper.findAll('button').forEach((button) => {
      expect(button.attributes('disabled')).toBeDefined();
    });
  });
});
