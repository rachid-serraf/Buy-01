import { fakeAsync, tick } from '@angular/core/testing';
import { CartItem } from './cart.mock';
import {
  announceCartAdded,
  cartBadgeBouncing,
  cartCount,
  cartDelivery,
  cartDrawerOpen,
  cartDtoToItems,
  cartItems,
  cartSubtotal,
  cartToast,
  cartTotal,
  closeCartDrawer,
  DELIVERY_FEE,
  freeDeliveryProgress,
  freeDeliveryRemaining,
  hideCartToast,
  isOutOfStock,
  openCartDrawer,
  removeCartItem,
  syncCart,
} from './cart-ui.state';

const item = (overrides: Partial<CartItem> = {}): CartItem => ({
  id: 'a',
  name: 'Item A',
  category: 'Home',
  price: 10,
  quantity: 1,
  stock: 5,
  image: 'data:,',
  ...overrides,
});

describe('cart UI state', () => {
  beforeEach(() => {
    cartItems.set([]);
    cartDrawerOpen.set(false);
    cartBadgeBouncing.set(false);
    hideCartToast();
  });

  afterEach(() => hideCartToast());

  describe('totals', () => {
    it('counts lines, not units', () => {
      cartItems.set([item({ quantity: 4 }), item({ id: 'b' })]);
      expect(cartCount()).toBe(2);
    });

    it('multiplies price by quantity in the subtotal', () => {
      cartItems.set([item({ price: 4, quantity: 5 }), item({ id: 'b', price: 18 })]);
      expect(cartSubtotal()).toBe(38);
    });

    it('leaves out-of-stock lines out of the subtotal', () => {
      cartItems.set([item({ price: 18 }), item({ id: 'b', price: 24, stock: 0 })]);
      expect(cartSubtotal()).toBe(18);
    });

    it('recognises an out-of-stock item', () => {
      expect(isOutOfStock(item({ stock: 0 }))).toBeTrue();
      expect(isOutOfStock(item({ stock: 1 }))).toBeFalse();
    });

    it('charges delivery below the free threshold and adds it to the total', () => {
      cartItems.set([item({ price: 38 })]);
      expect(cartDelivery()).toBe(DELIVERY_FEE);
      expect(cartTotal()).toBeCloseTo(38 + DELIVERY_FEE, 2);
    });

    it('gives free delivery exactly at the threshold', () => {
      cartItems.set([item({ price: 50 })]);
      expect(cartDelivery()).toBe(0);
      expect(cartTotal()).toBe(50);
    });

    it('charges no delivery on an empty cart', () => {
      expect(cartDelivery()).toBe(0);
      expect(cartTotal()).toBe(0);
    });
  });

  describe('free delivery progress', () => {
    it('reports how far away free delivery is', () => {
      cartItems.set([item({ price: 38 })]);
      expect(freeDeliveryRemaining()).toBe(12);
      expect(freeDeliveryProgress()).toBe(76);
    });

    it('stops at zero remaining and 100 percent', () => {
      cartItems.set([item({ price: 80 })]);
      expect(freeDeliveryRemaining()).toBe(0);
      expect(freeDeliveryProgress()).toBe(100);
    });
  });

  describe('changing lines', () => {
    it('removes a line', () => {
      cartItems.set([item(), item({ id: 'b' })]);
      removeCartItem('a');
      expect(cartItems().map((line) => line.id)).toEqual(['b']);
    });
  });

  describe('syncCart', () => {
    const cart = (quantity: number) => ({
      id: 'cart-1',
      userId: 'user-1',
      items: [
        {
          productId: 'a',
          sellerId: 'seller-1',
          name: 'Item A',
          unitPrice: 10,
          imageId: null,
          quantity,
        },
      ],
    });

    it('replaces the UI cart with the API cart', () => {
      cartItems.set([item({ id: 'stale', quantity: 9 })]);

      syncCart(cart(2));

      expect(cartItems()).toEqual([
        {
          id: 'a',
          name: 'Item A',
          category: '',
          price: 10,
          quantity: 2,
          stock: 2,
          image: './empty.png',
        },
      ]);
    });

    it('uses the quantity returned by the API when a line is re-added', () => {
      cartItems.set([item({ quantity: 5 })]);

      syncCart(cart(2));

      expect(cartItems()[0].quantity).toBe(2);
    });

    it('clears the cart when there is no cart', () => {
      cartItems.set([item()]);

      syncCart(null);

      expect(cartItems()).toEqual([]);
    });
  });

  describe('announceCartAdded', () => {
    const { quantity: _quantity, ...product } = item();

    it('bounces the badge and shows the toast', () => {
      announceCartAdded(product);
      expect(cartBadgeBouncing()).toBeTrue();
      expect(cartToast()).toEqual(product);
    });
  });

  describe('toast timing', () => {
    const { quantity: _quantity, ...product } = item();

    it('hides itself after four seconds', fakeAsync(() => {
      announceCartAdded(product);

      tick(3999);
      expect(cartToast()).not.toBeNull();

      tick(1);
      expect(cartToast()).toBeNull();
    }));

    it('restarts the timer when another item is added', fakeAsync(() => {
      announceCartAdded(product);
      tick(3000);
      announceCartAdded(product);

      tick(3000);
      expect(cartToast()).not.toBeNull();

      tick(1000);
      expect(cartToast()).toBeNull();
    }));
  });

  describe('cartDtoToItems', () => {
    it('maps the API cart into cart lines', () => {
      const items = cartDtoToItems({
        id: 'cart-1',
        userId: 'user-1',
        items: [
          {
            productId: 'product-1',
            sellerId: 'seller-1',
            name: 'Phone',
            unitPrice: 9.99,
            imageId: 'image-1',
            quantity: 2,
          },
        ],
      });

      expect(items).toEqual([
        {
          id: 'product-1',
          name: 'Phone',
          category: '',
          price: 9.99,
          quantity: 2,
          stock: 2,
          image: 'https://localhost:8080/media/image-1',
        },
      ]);
    });

    it('returns an empty list when there is no cart', () => {
      expect(cartDtoToItems(null)).toEqual([]);
    });
  });

  describe('drawer', () => {
    it('opens and closes', () => {
      openCartDrawer();
      expect(cartDrawerOpen()).toBeTrue();

      closeCartDrawer();
      expect(cartDrawerOpen()).toBeFalse();
    });

    it('hides the toast when it opens', () => {
      const { quantity: _quantity, ...product } = item();
      announceCartAdded(product);

      openCartDrawer();

      expect(cartToast()).toBeNull();
    });
  });
});
