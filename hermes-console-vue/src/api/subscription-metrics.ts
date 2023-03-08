export interface SubscriptionMetrics {
  delivered: number;
  discarded: number;
  volume: number;
  timeouts: string;
  otherErrors: string;
  codes2xx: string;
  codes4xx: string;
  codes5xx: string;
  lag: string;
  rate: string;
  throughput: string;
  batchRate: string;
}
