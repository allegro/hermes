import { dummyConstraints } from '@/dummy/constraints';
import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import ConstraintsListing from '@/views/admin/constraints/constraints-listing/ConstraintsListing.vue';

describe('ConstraintsListing', () => {
  it('should render topic constraints listing with no filter applied', () => {
    // given
    const props = {
      constraints: dummyConstraints.topicConstraints,
    };

    // when
    const { getByText } = render(ConstraintsListing, { props });

    // then
    Object.entries(dummyConstraints.topicConstraints).forEach(
      ([key, value], index) => {
        const constraintRow = getByText(key).closest('tr')!;
        expect(
          within(constraintRow).getByText(`${index + 1}`),
        ).toBeInTheDocument();
        expect(
          within(constraintRow).getByText(
            `constraints.listing.consumersNumberChip ${value.consumersNumber}`,
          ),
        ).toBeInTheDocument();
      },
    );
  });

  it('should render subscription constraints listing with no filter applied', () => {
    // given
    const props = {
      constraints: dummyConstraints.subscriptionConstraints,
    };

    // when
    const { getByText } = render(ConstraintsListing, { props });

    // then
    Object.entries(dummyConstraints.subscriptionConstraints).forEach(
      ([key, value], index) => {
        const constraintRow = getByText(key).closest('tr')!;
        expect(
          within(constraintRow).getByText(`${index + 1}`),
        ).toBeInTheDocument();
        expect(
          within(constraintRow).getByText(
            `constraints.listing.consumersNumberChip ${value.consumersNumber}`,
          ),
        ).toBeInTheDocument();
      },
    );
  });

  it('should render constraints listing with a filter applied', () => {
    // given
    const props = {
      constraints: dummyConstraints.topicConstraints,
      filter: 'pl.group.Topic1',
    };

    // when
    const constraints = render(ConstraintsListing, { props })
      .getAllByText(/pl\.group\.Topic1/)
      .map((constraint) => constraint.closest('tr'));

    // then
    expect(constraints).toHaveLength(1);
    expect(
      within(constraints[0]!).getByText(/consumersNumberChip 2/),
    ).toBeInTheDocument();
  });

  it('should render constraints listing with a filter applied (no results)', () => {
    // given
    const props = {
      constraints: dummyConstraints.topicConstraints,
      filter: 'pl.group.NoExistingTopic',
    };

    // when
    const rows = render(ConstraintsListing, { props }).getAllByRole('row');

    // then
    expect(rows).toHaveLength(2);
    expect(within(rows[0]!).getByText(/index/)).toBeInTheDocument();
    expect(within(rows[0]!).getByText(/name/)).toBeInTheDocument();
    expect(within(rows[1]!).getByText(/noConstraints/)).toBeInTheDocument();
    expect(within(rows[1]!).getByText(/appliedFilter/)).toBeInTheDocument();
  });

  it('should render an empty constraint listing without filter applied', () => {
    // given
    const props = {
      constraints: {},
    };

    // when
    const rows = render(ConstraintsListing, { props }).getAllByRole('row');

    // then
    expect(rows).toHaveLength(2);
    expect(within(rows[1]!).getByText(/noConstraints/)).toBeInTheDocument();
    expect(
      within(rows[1]!).queryByText(/appliedFilter/),
    ).not.toBeInTheDocument();
  });
});
