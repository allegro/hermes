import { ContentType } from '@/api/content-type';
import { describe, expect } from 'vitest';
import { dummyTopic } from '@/dummy/topic';
import { render } from '@/utils/test-utils';
import SchemaPanel from '@/views/topic/schema-panel/SchemaPanel.vue';
import userEvent from '@testing-library/user-event';

describe('SchemaPanel', () => {
  const props = {
    schema: dummyTopic.schema,
    contentType: ContentType.AVRO,
    topicName: dummyTopic.name,
    schemaRegistryUrl: 'https://schema-registry.example.com',
    schemaSubject: dummyTopic.name,
  };

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

  it('should show sorted, linked schema version history and current version', async () => {
    const { getByText, getByRole } = render(SchemaPanel, {
      props: {
        ...props,
        schemaVersion: 4,
        availableSchemaVersions: [4, 2, 1],
        schemaRegistryUrl: 'https://schema-registry.example.com/',
        topicName: 'group/topic name',
        schemaSubject: 'namespace_group/topic name-value',
      },
    });

    expect(
      getByText('topicView.schema.activeVersion', { exact: false }),
    ).toBeVisible();
    expect(getByText('4', { selector: 'strong' })).toBeVisible();
    expect(
      getByRole('button', {
        name: 'topicView.schema.allVersions',
      }),
    );
    await userEvent.click(
      getByText('topicView.schema.allVersions', { exact: false }),
    );

    const links = getByRole('list').querySelectorAll('a');
    expect([...links].map((link) => link.textContent?.trim())).toEqual([
      '4 topicView.schema.current',
      '2',
      '1',
    ]);
    expect(links[1]).toHaveAttribute(
      'href',
      'https://schema-registry.example.com/subjects/namespace_group%2Ftopic%20name-value/versions/2',
    );
    expect(links[1]).toHaveAttribute('target', '_blank');
    expect(links[1]).toHaveAttribute('rel', 'noopener noreferrer');
    expect(getByText('topicView.schema.current')).toBeVisible();
  });

  it('should show only the active version when version history is not available', () => {
    const { getByText, queryByText } = render(SchemaPanel, {
      props: { ...props, schemaVersion: 2 },
    });

    expect(
      getByText('topicView.schema.activeVersion', { exact: false }),
    ).toBeVisible();
    expect(
      queryByText('topicView.schema.allVersions', { exact: false }),
    ).not.toBeInTheDocument();
  });

  it('should show JSON topics as not applicable', () => {
    const { getByText, queryByText } = render(SchemaPanel, {
      props: {
        ...props,
        contentType: ContentType.JSON,
      },
    });

    expect(getByText('topicView.schema.notApplicable')).toBeVisible();
    expect(
      queryByText('topicView.schema.allVersions', { exact: false }),
    ).not.toBeInTheDocument();
  });

  it('should cap long version histories with scrolling', async () => {
    const { getByText, getByTestId } = render(SchemaPanel, {
      props: {
        ...props,
        availableSchemaVersions: Array.from(
          { length: 11 },
          (_, index) => index + 1,
        ),
      },
    });

    await userEvent.click(
      getByText('topicView.schema.allVersions', { exact: false }),
    );

    expect(getByTestId('schema-version-history')).toHaveClass(
      'schema-version-history',
    );
  });
});
