import {
  Order,
  OrderAddress,
  OrderStatus,
  OrderStatusMeta,
} from '../../../core/models/order.model';
import { Role } from '../../../core/models/user.model';

export const ORDER_STATUS_META: readonly OrderStatusMeta[] = [
  {
    status: 'PENDING',
    label: 'Pending',
    description: 'The order has been placed and is waiting for the seller.',
  },
  {
    status: 'CONFIRMED',
    label: 'Confirmed',
    description: 'The seller confirmed the order and is preparing it.',
  },
  {
    status: 'SHIPPED',
    label: 'Shipped',
    description: 'The order has left the seller and is on its way.',
  },
  {
    status: 'DELIVERED',
    label: 'Delivered',
    description: 'The order was delivered successfully.',
  },
  {
    status: 'CANCELLED',
    label: 'Cancelled',
    description: 'This order was cancelled and will not be fulfilled.',
  },
];

export const ORDER_FLOW: readonly OrderStatusMeta[] = ORDER_STATUS_META.filter(
  (meta) => meta.status !== 'CANCELLED',
);

export const ORDER_STATUS_OPTIONS = ORDER_STATUS_META.map((meta) => ({
  value: meta.status,
  label: meta.label,
}));

const SELLER_TRANSITIONS: Record<
  OrderStatus,
  { status: OrderStatus; label: string } | null
> = {
  PENDING: { status: 'CONFIRMED', label: 'Confirm order' },
  CONFIRMED: { status: 'SHIPPED', label: 'Mark as shipped' },
  SHIPPED: { status: 'DELIVERED', label: 'Mark as delivered' },
  DELIVERED: null,
  CANCELLED: null,
};

export function statusLabel(status: OrderStatus): string {
  return ORDER_STATUS_META.find((meta) => meta.status === status)?.label ?? status;
}

export function statusDescription(status: OrderStatus): string {
  return ORDER_STATUS_META.find((meta) => meta.status === status)?.description ?? '';
}

export function statusClass(status: OrderStatus): string {
  return `status-${status.toLowerCase()}`;
}

export function formatOrderDate(value?: string | null): string {
  const date = parseDate(value);
  if (!date) return '—';

  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(date);
}

export function formatOrderDateTime(value?: string | null): string {
  const date = parseDate(value);
  if (!date) return '—';

  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(date);
}

export function formatMoney(value: number): string {
  const amount = Number.isFinite(value) ? value : 0;
  return `$${amount.toFixed(2)}`;
}

export function orderItemsCount(order: Order): number {
  return order.items.reduce((total, item) => total + (item.quantity || 0), 0);
}

export function orderSubtotal(order: Order): number {
  return order.items.reduce(
    (total, item) => total + (item.price || 0) * (item.quantity || 0),
    0,
  );
}

export function orderDeliveryFee(order: Order): number {
  return Math.max(0, (order.total || 0) - orderSubtotal(order));
}

export function formatAddressLines(address?: string | OrderAddress | null): string[] {
  if (!address) return [];

  if (typeof address === 'string') {
    return address
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean);
  }

  const cityLine = [address.postalCode, address.city].filter(Boolean).join(' ');
  const lines = [
    address.fullName,
    address.phone,
    address.line1,
    address.line2,
    cityLine,
    address.state,
    address.country,
  ];

  return lines.filter((line): line is string => Boolean(line?.trim()));
}

export function canCancelOrder(order: Order, role: Role): boolean {
  if (role !== 'CLIENT' && role !== 'SELLER') return false;
  return order.status === 'PENDING' || order.status === 'CONFIRMED';
}

export function canRedoOrder(order: Order, role: Role): boolean {
  if (role !== 'CLIENT') return false;
  return order.status === 'CANCELLED' || order.status === 'DELIVERED';
}

export function canRemoveOrder(order: Order, role: Role): boolean {
  return canRedoOrder(order, role);
}

export function nextOrderStatus(
  order: Order,
  role: Role,
): { status: OrderStatus; label: string } | null {
  if (role !== 'SELLER') return null;
  return SELLER_TRANSITIONS[order.status];
}

export function readApiError(error: unknown, fallback: string): string {
  const body = (error as { error?: unknown } | null)?.error;

  if (typeof body === 'string' && body.trim()) {
    return body;
  }

  if (body && typeof body === 'object') {
    const record = body as Record<string, unknown>;
    const message = firstString(
      record['message'],
      record['detail'],
      record['title'],
      record['details'],
    );

    if (message) return message;

    const details = record['details'];
    if (Array.isArray(details)) {
      for (const detail of details) {
        const detailMessage = firstString(
          detail,
          (detail as Record<string, unknown> | null)?.['message'],
        );
        if (detailMessage) return detailMessage;
      }
    }
  }

  const rawMessage = (error as { message?: unknown } | null)?.message;
  return typeof rawMessage === 'string' && rawMessage.trim() ? rawMessage : fallback;
}

function parseDate(value?: string | null): Date | null {
  if (!value) return null;

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function firstString(...values: unknown[]): string | null {
  for (const value of values) {
    if (typeof value === 'string' && value.trim()) return value;
  }
  return null;
}
