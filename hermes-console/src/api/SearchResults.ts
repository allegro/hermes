export interface SearchResults {
  totalCount: number;
  results: SearchResultItem[];
}

export type SearchResultType = 'TOPIC' | 'SUBSCRIPTION';

export type SearchResultItem =
  | SearchResultTopicItem
  | SearchResultSubscriptionItem;

export interface SearchResultTopicItem {
  type: 'TOPIC';
  name: string;
  topic: TopicItemDetails;
}

export interface TopicItemDetails {
  owner: TopicOwnerDetails;
}

export interface TopicOwnerDetails {
  id: string;
}

export interface SearchResultSubscriptionItem {
  type: 'SUBSCRIPTION';
  name: string;
  subscription: SubscriptionItemDetails;
}

export interface SubscriptionItemDetails {
  topicName: string;
}
