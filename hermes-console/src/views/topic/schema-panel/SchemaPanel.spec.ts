import { describe, expect } from 'vitest';
import { dummyTopic } from '@/dummy/topic';
import { render } from '@/utils/test-utils';
import SchemaPanel from '@/views/topic/schema-panel/SchemaPanel.vue';
import userEvent from '@testing-library/user-event';

describe('SchemaPanel', () => {
  const props = { schema: dummyTopic.schema };

  it('should render avro formatted schema by default', async () => {
    // given
    const { getByTestId } = render(SchemaPanel, { props });

    // when
    const codeElement = getByTestId('avro-viewer');

    // then
    expect(codeElement).toBeVisible();
  });

  it('should render avro json schema', async () => {
    // given
    const { getByTestId, getByText } = render(SchemaPanel, { props });

    // when
    await userEvent.click(getByText('topicView.schema.rawSchema'));
    const codeElement = getByTestId('json-viewer')!!;

    // then
    expect(codeElement).toBeVisible();
    expect(codeElement.textContent).toEqual(
      JSON.stringify(JSON.parse(props.schema), null, 2),
    );
  });

  it('should go back to avro formatted schema', async () => {
    // given
    const { getByTestId, getByText } = render(SchemaPanel, { props });

    // when
    await userEvent.click(getByText('topicView.schema.rawSchema'));
    await userEvent.click(getByText('topicView.schema.structure'));
    const codeElement = getByTestId('avro-viewer');

    // then
    expect(codeElement).toBeVisible();
  });
});
