export interface CartItem {
  id: string;
  name: string;
  category: string;
  price: number;
  quantity: number;
  stock: number;
  image: string;
}

const artwork = (background: string, shape: string): string =>
  'data:image/svg+xml,' +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120">` +
      `<rect width="120" height="120" fill="${background}"/>${shape}</svg>`,
  );


export const CART_MOCK_ITEMS: CartItem[] = [
  {
    id: 'mock-mug',
    name: 'Stoneware mug',
    category: 'Home',
    price: 18,
    quantity: 1,
    stock: 12,
    image: artwork(
      '#edf3fb',
      `<rect x="34" y="38" width="44" height="48" rx="7" fill="#0052bf"/>` +
        `<path d="M78 48h8a10 10 0 0 1 0 28h-8" fill="none" stroke="#0052bf" stroke-width="7"/>`,
    ),
  },
  {
    id: 'mock-notebook',
    name: 'Recycled pocket notebook',
    category: 'Stationery',
    price: 4,
    quantity: 5,
    stock: 5,
    image: artwork(
      '#f3f4f6',
      `<rect x="38" y="28" width="46" height="64" rx="5" fill="#374151"/>` +
        `<rect x="46" y="28" width="5" height="64" fill="#9ca3af"/>` +
        `<rect x="57" y="42" width="19" height="5" rx="2.5" fill="#e5e7eb"/>`,
    ),
  },
  {
    id: 'mock-candle',
    name: 'Soy wax candle',
    category: 'Home',
    price: 24,
    quantity: 1,
    stock: 0,
    image: artwork(
      '#fef3e2',
      `<rect x="40" y="52" width="40" height="40" rx="6" fill="#d97706"/>` +
        `<rect x="58" y="42" width="4" height="11" fill="#374151"/>` +
        `<path d="M60 22c7 8 8 13 0 20c-8-7-7-12 0-20z" fill="#f59e0b"/>`,
    ),
  },
];

export const MOCK_DEFAULT_STOCK = 10;
