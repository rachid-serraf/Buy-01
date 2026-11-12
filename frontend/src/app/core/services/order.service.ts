import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import {
  Order,
  OrderAddress,
  OrderItem,
  OrderPage,
  OrderQuery,
  OrderStatus,
} from '../models/order.model';

interface OrderItemDto {
  id?: string;
  productId?: string;
  name?: string;
  price?: unknown;
  unitPrice?: unknown;
  quantity?: unknown;
  imageUrl?: string | null;
  image?: string | null;
  imagesIds?: unknown[];
}

interface OrderDto {
  id?: string;
  _id?: string;
  checkoutId?: string;
  buyerId?: string;
  userId?: string;
  sellerId?: string;
  status?: string;
  items?: OrderItemDto[];
  total?: unknown;
  totalAmount?: unknown;
  address?: unknown;
  createdAt?: string;
  created_at?: string;
}

interface OrderPageDto {
  content?: OrderDto[];
  page?: number;
  number?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
}

const ORDER_STATUSES: readonly OrderStatus[] = [
  'PENDING',
  'CONFIRMED',
  'SHIPPED',
  'DELIVERED',
  'CANCELLED',
];

@Injectable({
  providedIn: 'root',
})
export class OrderService {
  readonly http = inject(HttpClient);

  private readonly parentPath = '/orders';

  getMyOrders(query: OrderQuery = {}): Observable<OrderPage> {
    return this.http
      .get<OrderPageDto>(`${this.parentPath}/me`, { params: this.buildParams(query) })
      .pipe(map((page) => this.normalizePage(page)));
  }

  getSellerOrders(query: OrderQuery = {}): Observable<OrderPage> {
    return this.http
      .get<OrderPageDto>(`${this.parentPath}/seller`, { params: this.buildParams(query) })
      .pipe(map((page) => this.normalizePage(page)));
  }

  getOrder(id: string): Observable<Order> {
    return this.http
      .get<OrderDto>(`${this.parentPath}/${id}`)
      .pipe(map((order) => this.normalizeOrder(order)));
  }

  buyNow(productId: string, quantity = 1, address: OrderAddress): Observable<Order> {
    return this.http
      .post<OrderDto>(`${this.parentPath}/buy`, { productId, quantity, address })
      .pipe(map((order) => this.normalizeOrder(order)));
  }

  updateStatus(id: string, status: OrderStatus): Observable<void> {
    return this.http.patch<void>(`${this.parentPath}/${id}/status`, { status });
  }

  cancelOrder(id: string): Observable<void> {
    return this.http.post<void>(`${this.parentPath}/${id}/cancel`, {});
  }

  redoOrder(id: string): Observable<void> {
    return this.http.post<void>(`${this.parentPath}/${id}/redo`, {});
  }

  removeOrder(id: string): Observable<void> {
    return this.http.delete<void>(`${this.parentPath}/${id}`);
  }

  private buildParams(query: OrderQuery): HttpParams {
    let params = new HttpParams();

    Object.entries(query).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    });

    return params;
  }

  private normalizePage(raw: OrderPageDto | OrderDto[]): OrderPage {
    if (Array.isArray(raw)) {
      const content = raw.map((order) => this.normalizeOrder(order));
      return {
        content,
        page: 0,
        size: content.length,
        totalElements: content.length,
        totalPages: content.length > 0 ? 1 : 0,
      };
    }

    const content = Array.isArray(raw.content)
      ? raw.content.map((order) => this.normalizeOrder(order))
      : [];
    const page = toNumber(raw.page ?? raw.number) ?? 0;
    const size = toNumber(raw.size) ?? content.length;
    const totalElements = toNumber(raw.totalElements) ?? content.length;
    const totalPages =
      toNumber(raw.totalPages) ??
      (size > 0 ? Math.ceil(totalElements / size) : content.length > 0 ? 1 : 0);

    return { content, page, size, totalElements, totalPages };
  }

  private normalizeOrder(dto: OrderDto): Order {
    const items = Array.isArray(dto.items)
      ? dto.items.map((item) => this.normalizeItem(item))
      : [];
    const computedTotal = items.reduce(
      (total, item) => total + item.price * item.quantity,
      0,
    );
    const explicitTotal = toNumber(dto.total ?? dto.totalAmount);

    return {
      id: dto.id ?? dto._id ?? '',
      checkoutId: dto.checkoutId,
      buyerId: dto.buyerId ?? dto.userId,
      sellerId: dto.sellerId,
      status: normalizeStatus(dto.status),
      items,
      total: explicitTotal ?? computedTotal,
      address: normalizeAddress(dto.address),
      createdAt: dto.createdAt ?? dto.created_at,
    };
  }

  private normalizeItem(item: OrderItemDto): OrderItem {
    const firstImageId = Array.isArray(item.imagesIds)
      ? item.imagesIds.find((id): id is string => typeof id === 'string')
      : null;

    return {
      productId: item.productId ?? item.id ?? '',
      name: item.name?.trim() || 'Product',
      imageUrl: item.imageUrl ?? item.image ?? firstImageId ?? null,
      price: toNumber(item.price ?? item.unitPrice) ?? 0,
      quantity: toNumber(item.quantity) ?? 1,
    };
  }
}

function normalizeStatus(value?: string): OrderStatus {
  const status = (value ?? '').trim().toUpperCase();

  if (status === 'CANCELED') return 'CANCELLED';
  if (status === 'COMPLETED') return 'DELIVERED';

  return ORDER_STATUSES.includes(status as OrderStatus)
    ? (status as OrderStatus)
    : 'PENDING';
}

function normalizeAddress(value: unknown): string | OrderAddress | null {
  if (!value) return null;
  if (typeof value === 'string') return value;
  if (typeof value !== 'object') return null;

  const raw = value as Record<string, unknown>;

  return {
    fullName: asString(raw['fullName'] ?? raw['name']),
    phone: asString(raw['phone']),
    line1: asString(raw['line1'] ?? raw['street'] ?? raw['addressLine1']),
    line2: asString(raw['line2'] ?? raw['addressLine2']),
    city: asString(raw['city']),
    state: asString(raw['state']),
    postalCode: asString(raw['postalCode'] ?? raw['zip']),
    country: asString(raw['country']),
  };
}

function toNumber(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) return value;

  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }

  if (value && typeof value === 'object') {
    const decimal = (value as { $numberDecimal?: unknown }).$numberDecimal;
    if (typeof decimal === 'string' && decimal.trim() !== '') {
      const parsed = Number(decimal);
      return Number.isFinite(parsed) ? parsed : null;
    }
  }

  return null;
}

function asString(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim() ? value : undefined;
}
