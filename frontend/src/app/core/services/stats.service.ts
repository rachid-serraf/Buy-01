import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BuyerStats, SellerStats } from '../models/stats.model';

@Injectable({
  providedIn: 'root',
})
export class StatsService {
  private readonly http = inject(HttpClient);
  private readonly basePath = '/orders/stats';

  getBuyerStats(): Observable<BuyerStats> {
    return this.http.get<BuyerStats>(`${this.basePath}/me`);
  }

  getSellerStats(): Observable<SellerStats> {
    return this.http.get<SellerStats>(`${this.basePath}/seller`);
  }
}
