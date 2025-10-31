import { Inject, Injectable } from '@angular/core';
import { NewsResponse } from '../Interface/NewsResponse';
import { NewsTypeResponse } from '../Interface/NewsTypeResponse';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root'
})
export class NewsService {

  baseUrl = `${environment.serverUrl}/stock/news`;
  public newResponse: NewsResponse;

  constructor(private http: HttpClient) {

  }

  getLatestStockNews(type?: string): Observable<any> {
    const url = type ? `${this.baseUrl}?type=${type}` : this.baseUrl;
    return this.http.get<any>(url);
  }

  getNewsTypes(): Observable<any> {
    return this.http.get<NewsTypeResponse>(`${environment.serverUrl}/stock/news-types`);
  }
}
