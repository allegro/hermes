export interface InactiveTopic {
  topic: string;
  lastPublishedTsMs: number;
  notificationTsMs: number[];
  whitelisted: boolean;
}
