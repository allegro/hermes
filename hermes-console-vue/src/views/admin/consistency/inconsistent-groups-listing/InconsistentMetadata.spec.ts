import { describe, expect } from 'vitest';
import { inconsistentMetadata } from '@/dummy/inconsistentMetadata';
import { render } from '@/utils/test-utils';
import InconsistentMetadata from '@/views/admin/consistency/inconsistent-groups-listing/InconsistentMetadata.vue';

describe('InconsistentMetadataView', () => {
  it('should render metadata inconsistencies', () => {
    const { getByText } = render(InconsistentMetadata, {
      props: {
        metadata: inconsistentMetadata,
      },
    });
    expect(
      getByText('consistency.inconsistentGroup.metadata.inconsistent'),
    ).toBeVisible();
  });

  it('should render banner with info that metadata are consistent', () => {
    const { getByText } = render(InconsistentMetadata, {
      props: {
        metadata: [],
      },
    });
    expect(
      getByText('consistency.inconsistentGroup.metadata.consistent'),
    ).toBeVisible();
  });
});
