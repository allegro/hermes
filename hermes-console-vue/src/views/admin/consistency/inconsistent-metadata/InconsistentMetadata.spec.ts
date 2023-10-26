import { describe, expect } from 'vitest';
import { inconsistentMetadata } from '@/dummy/inconsistentMetadata';
import { render } from '@/utils/test-utils';
import InconsistentMetadata from '@/views/admin/consistency/inconsistent-metadata/InconsistentMetadata.vue';

describe('InconsistentMetadataView', () => {
  it('should render metadata inconsistencies', () => {
    // when
    const { getByText } = render(InconsistentMetadata, {
      props: {
        metadata: inconsistentMetadata,
      },
    });

    // then
    expect(
      getByText('consistency.inconsistentGroup.metadata.inconsistent'),
    ).toBeVisible();
  });

  it('should render banner with info that metadata are consistent', () => {
    // when
    const { getByText } = render(InconsistentMetadata, {
      props: {
        metadata: [],
      },
    });

    // then
    expect(
      getByText('consistency.inconsistentGroup.metadata.consistent'),
    ).toBeVisible();
  });
});
