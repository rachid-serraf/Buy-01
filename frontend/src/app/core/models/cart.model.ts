export interface AddCartItemRequest {
  productId: string;
  quantity: number;
}

export interface CartItemDto {
  productId: string;
  sellerId: string;
  name: string;
  unitPrice: number;
  imageId: string | null;
  quantity: number;
}

export interface CartDto {
  id: string;
  userId: string;
  items: CartItemDto[];
  createdAt?: string;
  updatedAt?: string;
}

export type CartIssueType =
  | 'PRODUCT_UNAVAILABLE'
  | 'INSUFFICIENT_STOCK'
  | 'PRICE_CHANGED'
  | 'NAME_CHANGED'
  | 'IMAGE_CHANGED'
  | 'SELLER_CHANGED';

export interface CartItemIssue {
  productId: string;
  name: string;
  type: CartIssueType;
  message: string;
}

export interface CheckoutResponse {
  valid: boolean;
  issues: CartItemIssue[];
  cart: CartDto;
}
