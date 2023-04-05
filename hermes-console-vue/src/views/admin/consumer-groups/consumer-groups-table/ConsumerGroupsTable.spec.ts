import { dummyConsumerGroups } from '@/dummy/consumerGroups';
import { render } from '@/utils/test-utils';
import ConsumerGroupsTable from '@/views/admin/consumer-groups/consumer-groups-table/ConsumerGroupsTable.vue';

describe('ConsumerGroupsTable', () => {
  const props = {
    consumerGroups: dummyConsumerGroups,
  };

  it('should render consumerGroups table', () => {
    // when
    const { getByText } = render(ConsumerGroupsTable, { props });

    // then
    expect(getByText(/kafka-1 \(Stable\)/i)).toBeInTheDocument();
    expect(getByText(/kafka-2 \(Dead\)/i)).toBeInTheDocument();
  });
});
