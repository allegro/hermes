import { dummyGroup } from '@/dummy/groups';
import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import GroupTopicsListing from '@/views/group-topics/group-topics-listing/GroupTopicsListing.vue';

describe('GroupTopicsListing', () => {
  it('should render group topics listing with no filter applied', () => {
    // given
    const props = {
      group: dummyGroup,
    };

    // when
    const { getByText } = render(GroupTopicsListing, { props });

    // then
    dummyGroup.topics.forEach((name, index) => {
      const topicRow = getByText(name).closest('tr')!;
      expect(within(topicRow).getByText(`${index + 1}`)).toBeInTheDocument();
      expect(within(topicRow).getByText(name)).toBeInTheDocument();
    });
  });

  it.each(['v1', 'V1', 'productevent', 'ProductEvent', 'PRODUCTEVENT'])(
    'should render group topics listing with a filter applied (case-insensitive, filter: %s)',
    (filter: string) => {
      // given
      const props = {
        group: dummyGroup,
        filter,
      };

      // when
      const topics = render(GroupTopicsListing, { props })
        .getAllByText(/ProductEventV1/)
        .map((topic) => topic.closest('tr'));

      // then
      expect(topics).toHaveLength(1);
    },
  );

  it('should render group topics listing with a filter applied (no results)', () => {
    // given
    const props = {
      groups: dummyGroup,
      filter: 'pl.allegro.i18n.DummyEventV1',
    };

    // when
    const rows = render(GroupTopicsListing, { props }).getAllByRole('row');

    // then
    expect(rows).toHaveLength(2);
    expect(within(rows[0]!).getByText(/index/)).toBeInTheDocument();
    expect(
      within(rows[0]!).getByText('groups.groupTopicsListing.name'),
    ).toBeInTheDocument();
    expect(
      within(rows[1]!).getByText(/groups.groupTopicsListing.noTopics/),
    ).toBeInTheDocument();
    expect(
      within(rows[1]!).getByText(/groups.groupTopicsListing.appliedFilter/),
    ).toBeInTheDocument();
  });

  it('should render an empty group topics listing without filter applied', () => {
    // given
    const props = {
      groups: [],
    };

    // when
    const rows = render(GroupTopicsListing, { props }).getAllByRole('row');

    // then
    expect(rows).toHaveLength(2);
    expect(
      within(rows[1]!).getByText('groups.groupTopicsListing.noTopics'),
    ).toBeInTheDocument();
    expect(
      within(rows[1]!).queryByText('groups.groupTopicsListing.appliedFilter'),
    ).not.toBeInTheDocument();
  });
});
