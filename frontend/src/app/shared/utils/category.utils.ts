export type Category = string;

export function formatCategory(category?: Category | null): string {
  if (!category) return '';
  return category
    .toLowerCase()
    .split('_')
    .map(part => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}
