import { dummyInconsistentTopics } from '@/dummy/inconsistentTopics';
import { render } from '@/utils/test-utils';
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

  it('should render inconsistent topics listing with a filter applied', () => {
    // given
    const props = {
      inconsistentTopics: dummyInconsistentTopics,
      filter: 'Dummy',
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
  });

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
});
