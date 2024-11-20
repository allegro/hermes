export interface OfflineRetransmissionTask {
  type: string;
  sourceTopic: string;
  targetTopic: string;
  startTimestamp: string;
  endTimestamp: string;
}
