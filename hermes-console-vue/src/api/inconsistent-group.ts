export interface InconsistentGroup {
  name: string;
  inconsistentMetadata: InconsistentMedata[];
  inconsistentTopics: InconsistentTopic[];
}

export interface InconsistentTopic {
  name: string;
  inconsistentMetadata: InconsistentMedata[];
  inconsistentSubscriptions: InconsistentSubscription[];
}

export interface InconsistentSubscription {
  name: string;
  inconsistentMetadata: InconsistentMedata[];
}

export interface InconsistentMedata {
  datacenter: string;
  content?: string;
}
