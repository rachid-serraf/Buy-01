import { Category } from '../../shared/utils/category.utils';

export interface ProductCreateRequest {
  name: string,
  description: string,
  price: number,
  quantity: number,
  imagesIds: string[],
  category: Category,
}


export interface Product {
  name: string,
  price: number,
  userId: string
  oldPrice: number,
  rating: 4.8,
  reviewsCount: 124,
  quantity: number,
  description: string,
  mainImage: string,
  thumbnails: string[],
  category: Category,
}

export interface ProductListDto {
  id: string,
  name: string,
  price: number,
  image: string,
  category: Category,
  createdAt: string;
  badge?: 'New'
}