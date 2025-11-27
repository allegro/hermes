export interface SearchResults {
  totalCount: number;
  results: SearchResultItem[];
}

export type SearchResultType = 'TOPIC' | 'SUBSCRIPTION';

export interface SearchResultItem {
  type: SearchResultType;
  name: string;
}

export interface SearchResultTopicItem extends SearchResultItem {
  type: 'TOPIC';
  name: string;
}

export interface SearchResultSubscriptionItem extends SearchResultItem {
  type: 'SUBSCRIPTION';
  name: string;
  subscription: SubscriptionItemDetails;
}

export interface SubscriptionItemDetails {
  topicName: string;
}
