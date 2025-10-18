import type { MessagePreview } from '@/api/topic';

export interface ParsedMessagePreview extends MessagePreview {
  messageId: string | null;
  timestamp: number | null;
  parsedContent: MessagePartiallyParsedContent | null;
}

export interface MessagePartiallyParsedContent {
  __metadata: {
    messageId: string;
    timestamp: string;
  };
}

export interface SelectedRow<Item> {
  index: number;
  item: Item;
}
