export type CommandPaletteElement =
  | CommandPaletteSubheaderElement
  | CommandPaletteDividerElement
  | CommandPaletteItemElement;

export interface CommandPaletteSubheaderElement {
  type: 'subheader';
  id: string;
  title: string;
}

export interface CommandPaletteDividerElement {
  type: 'divider';
  id: string;
}

export interface CommandPaletteItemElement {
  type: 'item';
  id: string;
  title: string;
  subtitle: string;
  icon: string;
  label: string;
  labelColor: string;
  onClick?: () => void;
}
