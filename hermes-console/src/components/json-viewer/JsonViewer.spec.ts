import { expect } from 'vitest';
import { render } from '@/utils/test-utils';
import JsonViewer from './JsonViewer.vue';

describe('JsonViewer', () => {
  it('should render json content', () => {
    // given
    const jsonData = { a: 1, b: 'test' };
    const props = {
      json: JSON.stringify(jsonData),
    };

    // when
    const { container } = render(JsonViewer, { props });

    // then
    const preElement = container.querySelector('pre');
    expect(preElement).not.toBeNull();
    expect(preElement?.textContent).toEqual(
      JSON.stringify(jsonData, null, 2),
    );
  });

  it('should render a valid json', () => {
    // given
    const props = {
      json: '{"test": "test"}',
    };

    // when
    const { container } = render(JsonViewer, { props });

    // then
    const preElement = container.querySelector('pre');
    expect(preElement).not.toBeNull();
    expect(preElement?.textContent).toEqual(
      JSON.stringify(JSON.parse(props.json), null, 2),
    );
  });

  it('should handle empty json string', () => {
    // given
    const props = {
      json: '{}',
    };

    // when
    const { container } = render(JsonViewer, { props });

    // then
    const preElement = container.querySelector('pre');
    expect(preElement).not.toBeNull();
    expect(preElement?.textContent).toEqual(
      JSON.stringify({}, null, 2),
    );
  });
});
