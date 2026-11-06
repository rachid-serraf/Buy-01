import { fakeAsync, tick } from '@angular/core/testing';
import { CartItem } from './cart.mock';
import {
  addToCart,
  cartBadgeBouncing,
  cartCount,
  cartDelivery,
  cartDrawerOpen,
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
  setCartItemQuantity,
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
    it('keeps a quantity between 1 and the stock', () => {
      cartItems.set([item({ stock: 5 })]);

      setCartItemQuantity('a', 3);
      expect(cartItems()[0].quantity).toBe(3);

      setCartItemQuantity('a', 99);
      expect(cartItems()[0].quantity).toBe(5);

      setCartItemQuantity('a', 0);
      expect(cartItems()[0].quantity).toBe(1);
    });

    it('only changes the requested line', () => {
      cartItems.set([item(), item({ id: 'b', quantity: 2 })]);
      setCartItemQuantity('a', 4);
      expect(cartItems()[1].quantity).toBe(2);
    });

    it('removes a line', () => {
      cartItems.set([item(), item({ id: 'b' })]);
      removeCartItem('a');
      expect(cartItems().map((line) => line.id)).toEqual(['b']);
    });
  });

  describe('addToCart', () => {
    const { quantity: _quantity, ...product } = item();

    it('adds a new line with the requested quantity', () => {
      addToCart(product, 2);
      expect(cartItems()).toEqual([{ ...product, quantity: 2 }]);
    });

    it('defaults to a quantity of one', () => {
      addToCart(product);
      expect(cartItems()[0].quantity).toBe(1);
    });

    it('increases the quantity of a line that already exists', () => {
      cartItems.set([item({ quantity: 2 })]);
      addToCart(product, 2);
      expect(cartItems().length).toBe(1);
      expect(cartItems()[0].quantity).toBe(4);
    });

    it('never goes over the stock', () => {
      cartItems.set([item({ quantity: 4, stock: 5 })]);
      addToCart(product, 3);
      expect(cartItems()[0].quantity).toBe(5);
    });

    it('bounces the badge and shows the toast', () => {
      addToCart(product);
      expect(cartBadgeBouncing()).toBeTrue();
      expect(cartToast()).toEqual(product);
    });
  });

  describe('toast timing', () => {
    const { quantity: _quantity, ...product } = item();

    it('hides itself after four seconds', fakeAsync(() => {
      addToCart(product);

      tick(3999);
      expect(cartToast()).not.toBeNull();

      tick(1);
      expect(cartToast()).toBeNull();
    }));

    it('restarts the timer when another item is added', fakeAsync(() => {
      addToCart(product);
      tick(3000);
      addToCart(product);

      tick(3000);
      expect(cartToast()).not.toBeNull();

      tick(1000);
      expect(cartToast()).toBeNull();
    }));
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
      addToCart(product);

      openCartDrawer();

      expect(cartToast()).toBeNull();
    });
  });
});
