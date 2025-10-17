import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { Observable, Subject } from 'rxjs';
import { environment } from 'src/environments/environment';
import { CreditResponse } from '../Interface/CreditResponse';
import { MockStockResponse } from '../Interface/MockStockResponse';
import { PaymentHistoryResponse } from '../Interface/paymentHistoryResponse';
import { SellStocksResponse } from '../Interface/SellStocksResponse';
import { StockOrderResponse } from '../Interface/StockOrderResponse';
import { NGXLogger } from 'ngx-logger';

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  creditsUrl = `${environment.serverUrl}/user/money`;
  stockHistoryUrl = `${environment.serverUrl}/user/transactions`;
  mockStocksUrl = `${environment.serverUrl}/user/stock/all`;
  paymentHistoryUrl = `${environment.serverUrl}/user/credit-transactions`;
  buyStockUrl = `${environment.serverUrl}/user/buy`;
  sellStockUrl = `${environment.serverUrl}/user/sell`;
  currentStocksUrl = `${environment.serverUrl}/user/stocks`;

  Stock: { type: string; name: string; currPrice: string };
  dataChanged = new Subject<void>();
  constructor(
    private http: HttpClient,
    private toastr: ToastrService,
    private logger: NGXLogger,
  ) {}

  setStock(stock) {
    this.Stock = stock;
  }

  getStock() {
    return this.Stock;
  }

  buyStock(buyOrder: { symbol: string; units: number }) {
    this.logger.info('Attempting to buy stock with order details:', buyOrder);
    this.http.put(this.buyStockUrl, [buyOrder]).subscribe(
      (data) => {
        this.logger.info('Stock purchase successful!', data);
        this.toastr.success('Stock Bought !!', '', {
          closeButton: true,
          positionClass: 'toast-bottom-right',
        });
      },
      (error) => {
        this.logger.error('Stock purchase failed! Details:', error);
        this.toastr.error(error.error.errors.error + '', '', {
          closeButton: true,
          positionClass: 'toast-bottom-right',
        });
      },
    );
    this.dataChanged.next();
  }

  sellStock(sellOrder: { symbol: string; units: number }) {
    this.logger.info('Attempting to sell stock.', sellOrder);
    this.http.put(this.sellStockUrl, [sellOrder]).subscribe(
      (data) => {
        this.logger.info(`Successfully sold ${sellOrder.units} units of ${sellOrder.symbol}.`);
        this.toastr.success('Stock Sold !!', '', {
          closeButton: true,
          positionClass: 'toast-bottom-right',
        });
      },
      (error) => {
        this.logger.error('Stock sell failed! Details:', error);
        this.toastr.error(error.error.errors.error + '', '', {
          closeButton: true,
          positionClass: 'toast-bottom-right',
        });
      },
    );
    this.dataChanged.next();
  }

  addCredits(credit: number) {
    this.http.put(this.creditsUrl, { value: credit }).subscribe(
      (data) => {
        this.toastr.success('Credit Added !!', '', {
          closeButton: true,
          positionClass: 'toast-bottom-right',
        });
      },
      (error) => {
        console.log('Something went wrong !');
        this.toastr.error(error.error.errors.error + '', '', {
          closeButton: true,
          positionClass: 'toast-bottom-right',
        });
      },
    );
    this.dataChanged.next();
  }

  fetchUserCredit(): Observable<CreditResponse> {
    return this.http.get<CreditResponse>(this.creditsUrl);
  }

  fetchStockOrderHistory(): Observable<StockOrderResponse> {
    return this.http.get<StockOrderResponse>(this.stockHistoryUrl);
  }

  fetchPaymentHistory(): Observable<PaymentHistoryResponse> {
    return this.http.get<PaymentHistoryResponse>(this.paymentHistoryUrl);
  }

  fetchMockStocks(): Observable<MockStockResponse> {
    return this.http.get<MockStockResponse>(this.mockStocksUrl);
  }

  fetchCurrentStocks(): Observable<SellStocksResponse> {
    return this.http.get<SellStocksResponse>(this.currentStocksUrl);
  }
}
