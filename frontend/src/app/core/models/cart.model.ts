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
