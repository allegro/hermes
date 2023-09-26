export interface DatacenterReadiness {
  datacenter: string;
  status: 'READY' | 'NOT_READY' | 'UNDEFINED';
}

export interface Readiness {
  isReady: boolean;
}
