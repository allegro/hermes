import type { DatacenterReadiness } from '@/api/datacenter-readiness';

export const dummyDatacentersReadiness: DatacenterReadiness[] = [
  {
    datacenter: 'DC1',
    status: 'READY',
  },
  {
    datacenter: 'DC2',
    status: 'NOT_READY',
  },
];
