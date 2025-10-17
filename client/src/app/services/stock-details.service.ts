import { HttpClient } from '@angular/common/http';
import { Injectable, OnInit } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { peerStockModel } from '../models/peerStockModel';
import { predictPriceModel } from '../models/predictPriceModel';
import { StockDetailsModel } from '../models/stockDetailsModel';
import { stockTrendModel } from '../models/stockTrendModel';
import { PricePredictionResponse } from '../Interface/PricePredictionResponse';
import { PeopleStockResponse } from '../Interface/PeopleStockResponse';
import { StockTrendResponse } from '../Interface/StockTrendResponse';
import { StockDetailResponse } from '../Interface/StockDetailResponse';
import { GraphDataResponse } from '../Interface/GraphDataResponse';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root',
})
export class StockDetailsService {
  urlStockDetails: StockDetailsModel = new StockDetailsModel(
    '',
    '',
    '',
    '',
    '',
    '',
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    '',
    { fmt: '' },
    '',
    '',
    { fmt: '' },
  );

  stockDetailUrl = `${environment.serverUrl}/stock/details?symbol=`;
  peopleStockUrl = `${environment.serverUrl}/stock/recommended-stocks/`;
  stockTrendUrl = `${environment.serverUrl}/stock/trends/`;
  predictPriceUrl = `${environment.modelUrl}/prediction?symbol=`;
  graphDataUrl = `${environment.serverUrl}/stock/graph/`;

  symbol: string;
  dataChanged = new Subject<void>();
  graphChanged = new Subject<void>();
  graphType = 'line';
  peerStocks: peerStockModel[];
  predictPrice: predictPriceModel[];
  stockTrend: stockTrendModel[];
  priceTrend: boolean;
  stockName: string;

  constructor(private http: HttpClient) {}

  fetchStockDetails(symbol) {
    this.symbol = symbol;
    return this.http.get<StockDetailResponse>(this.stockDetailUrl + symbol);
  }

  fetchGraphData(symbol): Observable<GraphDataResponse> {
    return this.http.get<GraphDataResponse>(this.graphDataUrl + symbol + '?range=1y&interval=1d');
  }

  fetchStockAbout(symbol): Observable<StockDetailResponse> {
    return this.http.get<StockDetailResponse>(this.stockDetailUrl + symbol);
  }

  fetchPeerStock(symbol): Observable<PeopleStockResponse> {
    return this.http.get<PeopleStockResponse>(this.peopleStockUrl + symbol);
  }

  fetchStockTrend(symbol): Observable<StockTrendResponse> {
    return this.http.get<StockTrendResponse>(this.stockTrendUrl + this.symbol);
  }

  fetchPredictiedPrice(symbol): Observable<PricePredictionResponse> {
    return this.http.get<PricePredictionResponse>(this.predictPriceUrl + this.symbol);
  }

  setStockDetails(stockDetails) {
    this.urlStockDetails = stockDetails;
    this.setPriceTrend();
  }

  getStockDetails() {
    return this.urlStockDetails;
  }

  getCurrentPrice(): string {
    return this.urlStockDetails.currentPrice.fmt;
  }

  getGraphType() {
    return this.graphType;
  }

  setGraphType(type) {
    if (type == 'line') {
      this.graphType = 'line';
      this.graphChanged.next();
    } else if (type == 'candle') {
      this.graphType = 'candle';
      this.graphChanged.next();
    }
  }

  getStockLongName() {
    return this.stockName;
  }

  setStockLongName() {
    this.stockName = this.urlStockDetails.longName;
  }

  setPriceTrend() {
    this.priceTrend =
      Number(this.urlStockDetails.currentPrice.fmt) >=
      Number(this.urlStockDetails.previousClose.fmt)
        ? true
        : false;
    this.graphChanged.next();
  }

  getPriceTrend(): boolean {
    return this.priceTrend;
  }
}
