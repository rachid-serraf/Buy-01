import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CartDto } from '../models/cart.model';

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly parentPath = '/cart';

  addItem(productId: string, quantity = 1): Observable<CartDto> {
    return this.http.post<CartDto>(`${this.parentPath}/items`, { productId, quantity });
  }
}
