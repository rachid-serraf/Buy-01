export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface OrderItem {
  productId: string;
  name: string;
  imageUrl?: string | null;
  price: number;
  quantity: number;
}

export interface OrderAddress {
  fullName?: string;
  phone?: string;
  line1?: string;
  line2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
}

export interface Order {
  id: string;
  title?: string;
  checkoutId?: string;
  buyerId?: string;
  sellerId?: string;
  status: OrderStatus;
  items: OrderItem[];
  total: number;
  address?: string | OrderAddress | null;
  createdAt?: string;
}

export interface OrderQuery {
  search?: string;
  status?: OrderStatus;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export interface OrderPage {
  content: Order[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface OrderStatusMeta {
  status: OrderStatus;
  label: string;
  description: string;
}
