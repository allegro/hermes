export type FilterMatchingStrategy = 'all' | 'any';

export interface PathFilter {
  id: string;
  path: string;
  matcher: string;
  matchingStrategy: FilterMatchingStrategy;
}
