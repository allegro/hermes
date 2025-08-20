import type { MessagePreview } from '@/api/topic';

export interface ParsedMessagePreview extends MessagePreview {
  parsedContent: {
    __metadata: {
      messageId: string;
      timestamp: string;
    };
  };
}

export interface SelectedRow<Item> {
  index: number;
  item: Item;
}
