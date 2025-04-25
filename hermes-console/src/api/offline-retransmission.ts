export interface OfflineRetransmissionCreateTask {
  type: string;
  sourceTopic: string;
  targetTopic: string;
  startTimestamp: string;
  endTimestamp: string;
}

export interface OfflineRetransmissionActiveTask {
  type: string;
  taskId: string;
  logsUrl: string,
  metricsUrl: string,
  jobDetailsUrl: string
}
