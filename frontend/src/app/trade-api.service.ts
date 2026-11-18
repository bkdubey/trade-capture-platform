import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Trade {
  id: string;
  bookingSystem: string;
  tradeId: number;
  instrumentId: string;
  quantity: number;
  tradePrice: number;
  tradeTimestamp: string;
  source: 'FILE' | 'KAFKA';
  status: 'VALID' | 'INVALID';
  validationReason: string | null;
  receivedAt: string;
}

export interface TradePage {
  content: Trade[];
  totalElements: number;
  page: number;
  size: number;
}

export interface Summary {
  total: number;
  valid: number;
  invalid: number;
  file: number;
  kafka: number;
}

export interface Query {
  status: string;
  source: string;
  from: string;
  to: string;
  page: number;
  size: number;
  search: string;
  sortBy: string;
  sortDirection: 'ASC' | 'DESC';
}

@Injectable({ providedIn: 'root' })
export class TradeApiService {
  private readonly http = inject(HttpClient);

  getSummary(from: string, to: string): Observable<Summary> {
    let params = new HttpParams();
    if (from) params = params.set('from', new Date(from).toISOString());
    if (to) params = params.set('to', new Date(to).toISOString());
    return this.http.get<Summary>('/api/v1/reports/summary', { params });
  }

  getTrades(query: Query): Observable<TradePage> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.status) params = params.set('status', query.status);
    if (query.source) params = params.set('source', query.source);
    if (query.from) params = params.set('from', new Date(query.from).toISOString());
    if (query.to) params = params.set('to', new Date(query.to).toISOString());
    if (query.search) params = params.set('search', query.search);
    params = params.set('sortBy', query.sortBy).set('sortDirection', query.sortDirection);
    return this.http.get<TradePage>('/api/v1/trades', { params });
  }

  upload(file: File): Observable<{ accepted: number }> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<{ accepted: number }>('/api/v1/ingestion/files', form);
  }
}
