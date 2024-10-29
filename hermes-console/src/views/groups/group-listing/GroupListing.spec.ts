import { dummyGroups } from '@/dummy/groups';
import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import GroupListing from '@/views/groups/group-listing/GroupListing.vue';

describe('GroupListing', () => {
  it('should render group listing with no filter applied', () => {
    // given
    const props = {
      groups: dummyGroups,
    };

    // when
    const { getByText } = render(GroupListing, { props });

    // then
    dummyGroups.forEach(({ name, topics }, index) => {
      const groupRow = getByText(name).closest('tr')!;
      expect(within(groupRow).getByText(`${index + 1}`)).toBeInTheDocument();
      expect(within(groupRow).getByText(name)).toBeInTheDocument();
      expect(
        within(groupRow).getByText(
          `groups.groupListing.topicsChip ${topics.length}`,
        ),
      ).toBeInTheDocument();
    });
  });

  it.each(['pl.allegro.offer', 'PL.ALLEGRO.OFFER', 'Pl.Allegro.Offer'])(
    'should render group listing with a filter applied (case-insensitive, filter: %s)',
    (filter: string) => {
      // given
      const props = {
        groups: dummyGroups,
        filter,
      };

      // when
      const groups = render(GroupListing, { props })
        .getAllByText(/pl\.allegro\.offer/)
        .map((group) => group.closest('tr'));

      // then
      expect(groups).toHaveLength(2);
      expect(within(groups[0]!).getByText(/topicsChip 1/)).toBeInTheDocument();
      expect(within(groups[1]!).getByText(/topicsChip 2/)).toBeInTheDocument();
    },
  );

  it('should render group listing with a filter applied (no results)', () => {
    // given
    const props = {
      groups: dummyGroups,
      filter: 'pl.allegro.i18n.DummyEventV1',
    };

    // when
    const rows = render(GroupListing, { props }).getAllByRole('row');

    // then
    expect(rows).toHaveLength(2);
    expect(
      within(rows[0]!).getByText('groups.groupListing.index'),
    ).toBeInTheDocument();
    expect(
      within(rows[0]!).getByText('groups.groupListing.name'),
    ).toBeInTheDocument();
    expect(
      within(rows[1]!).getByText(/groups.groupListing.noGroups/),
    ).toBeInTheDocument();
    expect(
      within(rows[1]!).getByText(/groups.groupListing.appliedFilter/),
    ).toBeInTheDocument();
  });

  it('should render an empty group listing without filter applied', () => {
    // given
    const props = {
      groups: [],
    };

    // when
    const rows = render(GroupListing, { props }).getAllByRole('row');

    // then
    expect(rows).toHaveLength(2);
    expect(
      within(rows[1]!).getByText('groups.groupListing.noGroups'),
    ).toBeInTheDocument();
    expect(
      within(rows[1]!).queryByText('groups.groupListing.appliedFilter'),
    ).not.toBeInTheDocument();
  });
});
