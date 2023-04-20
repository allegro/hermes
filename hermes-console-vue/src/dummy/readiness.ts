import type { DatacenterReadiness } from '@/api/datacenter-readiness';

export const dummyDatacentersReadiness: DatacenterReadiness[] = [
  {
    datacenter: 'DC1',
    isReady: true,
  },
  {
    datacenter: 'DC2',
    isReady: false,
  },
];
