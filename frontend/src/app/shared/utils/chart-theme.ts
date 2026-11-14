
export const CHART_COLORS = [
  '#0052bf',
  '#2f7ad6',
  '#5a9ae6',
  '#86b9f2',
  '#b3d4fa',
  '#d9e9fd',
];

export const CHART_GRID_COLOR = 'rgba(148, 163, 184, 0.18)';
export const CHART_TEXT_COLOR = '#6b7280';

export function formatMonthLabel(month: string): string {
  const [year, monthNumber] = month.split('-').map(Number);

  if (!Number.isFinite(year) || !Number.isFinite(monthNumber)) {
    return month;
  }

  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    year: 'numeric',
  }).format(new Date(Date.UTC(year, monthNumber - 1, 1)));
}
