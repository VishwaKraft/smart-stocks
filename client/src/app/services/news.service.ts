import { Injectable } from '@angular/core';
import { NewsResponse } from '../Interface/NewsResponse';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root',
})
export class NewsService {
  baseUrl = `${environment.serverUrl}/stock/news`;
  public newResponse: NewsResponse;

  constructor(private http: HttpClient) {}

  getLatestStockNews(): Observable<any> {
    return this.http.get<any>(this.baseUrl);
  }
}
