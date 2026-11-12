import { Order, OrderStatus } from '../../../core/models/order.model';
import {
  canCancelOrder,
  canRedoOrder,
  canRemoveOrder,
  nextOrderStatus,
  readApiError,
} from './order.utils';

function orderWith(status: OrderStatus): Order {
  return {
    id: 'order-1',
    status,
    items: [{ productId: 'p-1', name: 'Product', price: 10, quantity: 1 }],
    total: 10,
  };
}

describe('order action rules', () => {
  it('allows buyers and sellers to cancel pending or confirmed orders', () => {
    expect(canCancelOrder(orderWith('PENDING'), 'CLIENT')).toBeTrue();
    expect(canCancelOrder(orderWith('CONFIRMED'), 'SELLER')).toBeTrue();
  });

  it('blocks cancelling shipped, delivered or cancelled orders', () => {
    expect(canCancelOrder(orderWith('SHIPPED'), 'SELLER')).toBeFalse();
    expect(canCancelOrder(orderWith('DELIVERED'), 'CLIENT')).toBeFalse();
    expect(canCancelOrder(orderWith('CANCELLED'), 'CLIENT')).toBeFalse();
  });

  it('allows only buyers to redo or remove cancelled and delivered orders', () => {
    expect(canRedoOrder(orderWith('CANCELLED'), 'CLIENT')).toBeTrue();
    expect(canRedoOrder(orderWith('DELIVERED'), 'CLIENT')).toBeTrue();
    expect(canRedoOrder(orderWith('DELIVERED'), 'SELLER')).toBeFalse();
    expect(canRemoveOrder(orderWith('CANCELLED'), 'CLIENT')).toBeTrue();
    expect(canRemoveOrder(orderWith('PENDING'), 'CLIENT')).toBeFalse();
  });

  it('moves sellers through the fulfilment flow', () => {
    expect(nextOrderStatus(orderWith('PENDING'), 'SELLER')?.status).toBe('CONFIRMED');
    expect(nextOrderStatus(orderWith('CONFIRMED'), 'SELLER')?.status).toBe('SHIPPED');
    expect(nextOrderStatus(orderWith('SHIPPED'), 'SELLER')?.status).toBe('DELIVERED');
    expect(nextOrderStatus(orderWith('DELIVERED'), 'SELLER')).toBeNull();
    expect(nextOrderStatus(orderWith('PENDING'), 'CLIENT')).toBeNull();
  });
});

describe('readApiError', () => {
  it('reads the API error message', () => {
    const error = {
      error: {
        code: 'ORDER_NOT_FOUND',
        message: 'Order does not exist',
        details: [],
      },
    };

    expect(readApiError(error, 'fallback')).toBe('Order does not exist');
  });

  it('falls back to the first detail message', () => {
    const error = {
      error: {
        code: 'VALIDATION_ERROR',
        details: [{ message: 'Status is invalid' }],
      },
    };

    expect(readApiError(error, 'fallback')).toBe('Status is invalid');
  });

  it('returns the fallback when no message can be found', () => {
    expect(readApiError({}, 'fallback')).toBe('fallback');
  });
});
