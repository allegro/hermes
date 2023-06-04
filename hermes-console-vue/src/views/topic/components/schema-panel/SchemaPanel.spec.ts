import { describe, expect } from 'vitest';
import { dummyTopic } from '@/dummy/topic';
import { render } from '@/utils/test-utils';
import SchemaPanel from '@/views/topic/components/schema-panel/SchemaPanel.vue';
import userEvent from '@testing-library/user-event';

describe('SchemaPanel', () => {
  const props = { schema: dummyTopic.schema };

  it('should render proper heading', () => {
    // when
    const { getByText } = render(SchemaPanel, { props });

    // when
    expect(getByText('topicView.schema.title')).toBeVisible();
  });

  it('should render schema', async () => {
    // given
    const { getByText, container } = render(SchemaPanel, { props });

    // when
    await userEvent.click(getByText('topicView.schema.title'));
    const codeElement = container.querySelector('.v-code')!!;

    // when
    expect(codeElement).toBeVisible();
    expect(codeElement.textContent).toEqual(
      JSON.stringify(JSON.parse(props.schema), null, 2),
    );
  });
});
