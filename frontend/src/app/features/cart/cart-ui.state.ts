import { computed, signal } from '@angular/core';
import { CartDto } from '../../core/models/cart.model';
import { env } from '../../../environments/environment';
import { CART_MOCK_ITEMS, CartItem } from './cart.mock';

export const FREE_DELIVERY_THRESHOLD = 50;
export const DELIVERY_FEE = 4.99;

const TOAST_DURATION_MS = 4000;

export type CartProduct = Omit<CartItem, 'quantity'>;

/** Maps the cart returned by the API into the shape used by the cart UI. */
export function cartDtoToItems(cart: CartDto | null): CartItem[] {
  if (!cart) return [];

  return cart.items.map((item) => ({
    id: item.productId,
    name: item.name,
    category: '',
    price: item.unitPrice,
    quantity: item.quantity,
    stock: Math.max(item.quantity, 1),
    image: item.imageId ? `${env.mediaUrl}/${item.imageId}` : './empty.png',
  }));
}

export const cartItems = signal<CartItem[]>([]);
export const cartDrawerOpen = signal(false);
export const cartBadgeBouncing = signal(false);
export const cartToast = signal<CartProduct | null>(null);

export const isOutOfStock = (item: CartItem): boolean => item.stock < 1;

export const cartCount = computed(() => cartItems().length);

export const cartSubtotal = computed(() =>
  cartItems()
    .filter((item) => !isOutOfStock(item))
    .reduce((sum, item) => sum + item.price * item.quantity, 0),
);

export const cartDelivery = computed(() => {
  const subtotal = cartSubtotal();
  return subtotal === 0 || subtotal >= FREE_DELIVERY_THRESHOLD ? 0 : DELIVERY_FEE;
});

export const cartTotal = computed(() => cartSubtotal() + cartDelivery());

export const freeDeliveryRemaining = computed(() =>
  Math.max(0, FREE_DELIVERY_THRESHOLD - cartSubtotal()),
);

export const freeDeliveryProgress = computed(() =>
  Math.min(100, Math.round((cartSubtotal() / FREE_DELIVERY_THRESHOLD) * 100)),
);

export function openCartDrawer(): void {
  hideCartToast();
  cartDrawerOpen.set(true);
}

export function closeCartDrawer(): void {
  cartDrawerOpen.set(false);
}

export function removeCartItem(id: string): void {
  cartItems.update((items) => items.filter((item) => item.id !== id));
}

/** Replaces the UI cart with the cart returned by the API. */
export function syncCart(cart: CartDto | null): void {
  cartItems.set(cartDtoToItems(cart));
}

/** Bounces the badge and shows the "added to cart" toast. */
export function announceCartAdded(product: CartProduct): void {
  cartBadgeBouncing.set(true);
  showCartToast(product);
}

let toastTimer: ReturnType<typeof setTimeout> | undefined;

function showCartToast(product: CartProduct): void {
  clearTimeout(toastTimer);
  cartToast.set(product);
  toastTimer = setTimeout(hideCartToast, TOAST_DURATION_MS);
}

export function hideCartToast(): void {
  clearTimeout(toastTimer);
  cartToast.set(null);
}
