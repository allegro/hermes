export interface SubscriptionMetrics {
  delivered: number;
  discarded: number;
  volume: number;
  timeouts: MetricDecimalValue;
  otherErrors: MetricDecimalValue;
  codes2xx: MetricDecimalValue;
  codes4xx: MetricDecimalValue;
  codes5xx: MetricDecimalValue;
  lag: MetricLongValue;
  rate: MetricDecimalValue;
  throughput: MetricDecimalValue;
  batchRate: MetricDecimalValue;
}

export type MetricDecimalValue = string;
export type MetricLongValue = string;
