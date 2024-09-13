import { beforeEach, describe, expect } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { inconsistentMetadata } from '@/dummy/inconsistentMetadata';
import { render, renderWithEmits } from '@/utils/test-utils';
import { waitFor } from '@testing-library/vue';
import InconsistentMetadata from '@/views/admin/consistency/inconsistent-metadata/InconsistentMetadata.vue';
import router from '@/router';

describe('InconsistentMetadataView', () => {
  beforeEach(async () => {
    setActivePinia(createPinia());
  });

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

  it('should emit event with selected datacenter when sync button is clicked', async () => {
    const group = 'pl.allegro.public.group';
    await router.push(`/ui/consistency/${group}`);

    const wrapper = renderWithEmits(InconsistentMetadata, {
      props: {
        metadata: inconsistentMetadata,
      },
    });

    const buttonId = `sync-datacenter-${inconsistentMetadata[0].datacenter}`;
    await wrapper.find(`[data-testid="${buttonId}"]`).trigger('click');

    await waitFor(() => {
      const syncEvents = wrapper.emitted('sync')!!;
      expect(syncEvents.length).toEqual(1);
      expect(syncEvents[0]).toEqual([inconsistentMetadata[0].datacenter]);
    });
  });
});
