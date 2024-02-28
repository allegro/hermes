export interface DatacenterReadiness {
  datacenter: string;
  status: 'READY' | 'NOT_READY';
}

export interface Readiness {
  isReady: boolean;
}
