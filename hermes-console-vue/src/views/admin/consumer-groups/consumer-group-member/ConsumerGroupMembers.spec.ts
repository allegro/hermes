import { dummyConsumerGroups } from '@/dummy/consumerGroups';
import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import ConsumerGroupMembers from '@/views/admin/consumer-groups/consumer-group-member/ConsumerGroupMembers.vue';

describe('ConsumerGroupMembers', () => {
  const props = {
    members: dummyConsumerGroups[0].members,
  };

  it('should render members table', () => {
    // when
    const rows = render(ConsumerGroupMembers, { props }).getAllByRole('row');

    // then
    expect(rows).toHaveLength(4);
    expect(within(rows[0]!).getByText(/type/)).toBeVisible();
    expect(within(rows[0]!).getByText(/partition/)).toBeVisible();
    expect(within(rows[0]!).getByText(/currentOffset/)).toBeVisible();
    expect(within(rows[0]!).getByText(/endOffset/)).toBeVisible();
    expect(within(rows[0]!).getByText(/lag/)).toBeVisible();

    props.members.forEach((member, index) => {
      expect(
        within(rows[index + 1]).getByText(/host 123\.11\.22\.33/),
      ).toBeVisible();
      member.partitions.forEach(
        ({ partition, contentType }, partitionIndex) => {
          expect(
            within(rows[index + partitionIndex + 2]).getByText(partition),
          ).toBeVisible();
          expect(
            within(rows[index + partitionIndex + 2]).getByText(contentType),
          ).toBeVisible();
        },
      );
    });
  });
});
