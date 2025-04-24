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
  sourceViewPath: string | null;
  sourceTopic: string | null;
  targetTopic: string;
  startTimestamp: string;
  endTimestamp: string;
  createdAt: string;
}
