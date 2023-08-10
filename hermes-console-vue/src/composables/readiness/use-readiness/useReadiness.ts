import { fetchReadiness as getReadiness } from '@/api/hermes-client';
import { ref } from 'vue';
import type { DatacenterReadiness } from '@/api/datacenter-readiness';
import type { Ref } from 'vue';

export interface UseReadiness {
  datacentersReadiness: Ref<DatacenterReadiness[] | undefined>;
  loading: Ref<boolean>;
  error: Ref<UseReadinessErrors>;
}

export interface UseReadinessErrors {
  fetchReadiness: Error | null;
}

export function useReadiness(): UseReadiness {
  const datacentersReadiness = ref<DatacenterReadiness[]>();
  const error = ref<UseReadinessErrors>({
    fetchReadiness: null,
  });
  const loading = ref(false);

  const fetchReadiness = async () => {
    try {
      loading.value = true;
      datacentersReadiness.value = (await getReadiness()).data;
    } catch (e) {
      error.value.fetchReadiness = e as Error;
    } finally {
      loading.value = false;
    }
  };

  fetchReadiness();

  return {
    datacentersReadiness,
    loading,
    error,
  };
}
