import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CartDto, CheckoutResponse } from '../models/cart.model';
import { OrderAddress } from '../models/order.model';

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly parentPath = '/cart';

  getCart(): Observable<CartDto | null> {
    return this.http.get<CartDto | null>(`${this.parentPath}/items`);
  }

  addItem(productId: string, quantity = 1): Observable<CartDto> {
    return this.http.post<CartDto>(`${this.parentPath}/items`, { productId, quantity });
  }

  removeItem(productId: string): Observable<CartDto> {
    return this.http.delete<CartDto>(`${this.parentPath}/items/${productId}`);
  }

  checkout(address: OrderAddress): Observable<CheckoutResponse> {
    return this.http.post<CheckoutResponse>(`${this.parentPath}/checkout`, { address });
  }
}
