import { dummyTopic } from '@/dummy/topic';
import { render } from '@/utils/test-utils';
import { within } from '@testing-library/vue';
import TopicSearchResults from '@/views/search/topic-search-results/TopicSearchResults.vue';

describe('TopicSearchResults', () => {
  const props = {
    topics: [dummyTopic],
  };

  it('should render topics table', () => {
    // when
    const rows = render(TopicSearchResults, { props }).getAllByRole('row');
    expect(rows).toHaveLength(2);
    expect(
      within(rows[0]!).getByText('search.results.topic.name'),
    ).toBeVisible();
    expect(
      within(rows[0]!).getByText('search.results.topic.owner'),
    ).toBeVisible();

    props.topics.forEach((topic, index) => {
      expect(within(rows[index + 1]).getByText(topic.name)).toBeVisible();
    });
  });
});
