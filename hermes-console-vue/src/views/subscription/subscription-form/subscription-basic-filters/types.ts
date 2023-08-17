export type FilterMatchingStrategy = 'all' | 'any';

export interface Filter {
  id: string;
  path: string;
  matcher: string;
  matchingStrategy: FilterMatchingStrategy;
}
