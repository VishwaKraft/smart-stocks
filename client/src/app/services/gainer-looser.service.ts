import { Injectable } from '@angular/core';
import { GainerResponse } from '../Interface/GainerResponse';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root',
})
export class GainerLooserService {
  baseUrl = `${environment.modelUrl}/data?type=`;
  recommendatedFiveUrl = `${environment.serverUrl}/stock/recommended-stocks/`;
  allStocksUrl = `${environment.serverUrl}/stock/all?pageNo=`;
  topUrl = `${environment.serverUrl}/stock/top?days=`;

  public GainerResponse: GainerResponse;

  constructor(private http: HttpClient) {}

  getTopGainer(): Observable<GainerResponse> {
    return this.http.get<GainerResponse>(this.baseUrl + 'gainers');
  }

  getTopLooser(): Observable<GainerResponse> {
    return this.http.get<GainerResponse>(this.baseUrl + 'losers');
  }

  getTopRecommendation(sym: string): Observable<any> {
    return this.http.get<any>(this.recommendatedFiveUrl + sym);
  }

  getallStocks(pageNO: any): Observable<any> {
    return this.http.get<any>(this.allStocksUrl + pageNO + '&size=10');
  }

  getTopInXdays(days: any, params: string): Observable<any> {
    return this.http.get<any>(this.topUrl + days + '&type=' + params);
  }
}
