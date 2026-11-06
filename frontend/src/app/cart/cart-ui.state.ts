// TODO buy-02: replace with CartService + API
//
// UI-only state shared by the navbar, drawer, cart page, product cards and toast.
// Plain signals on purpose: no @Injectable, no HTTP, nothing is persisted.

import { computed, signal } from '@angular/core';
import { CART_MOCK_ITEMS, CartItem } from './cart.mock';

export const FREE_DELIVERY_THRESHOLD = 50;
export const DELIVERY_FEE = 4.99;

const TOAST_DURATION_MS = 4000;

export type CartProduct = Omit<CartItem, 'quantity'>;

export const cartItems = signal<CartItem[]>(CART_MOCK_ITEMS);
export const cartDrawerOpen = signal(false);
export const cartBadgeBouncing = signal(false);
export const cartToast = signal<CartProduct | null>(null);

export const isOutOfStock = (item: CartItem): boolean => item.stock < 1;

export const cartCount = computed(() => cartItems().length);

/** Out-of-stock lines stay visible in the cart but can't be bought, so they aren't charged. */
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

export function setCartItemQuantity(id: string, quantity: number): void {
  cartItems.update((items) =>
    items.map((item) =>
      item.id === id
        ? { ...item, quantity: Math.min(item.stock, Math.max(1, quantity)) }
        : item,
    ),
  );
}

export function removeCartItem(id: string): void {
  cartItems.update((items) => items.filter((item) => item.id !== id));
}

export function addToCart(product: CartProduct, quantity = 1): void {
  cartItems.update((items) => {
    const existing = items.find((item) => item.id === product.id);
    if (!existing) return [...items, { ...product, quantity }];
    return items.map((item) =>
      item === existing
        ? { ...item, quantity: Math.min(item.stock, item.quantity + quantity) }
        : item,
    );
  });
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
