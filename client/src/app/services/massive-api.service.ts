import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from 'src/environments/environment';

// ─── Response Interfaces ──────────────────────────────────────────────────────

export interface MassiveTicker {
  ticker: string;
  name: string;
  market: string;
  locale: string;
  primary_exchange: string;
  type: string;
  active: boolean;
  currency_name: string;
  cik?: string;
  composite_figi?: string;
  share_class_figi?: string;
  last_updated_utc: string;
}

export interface MassiveTickersResponse {
  results: MassiveTicker[];
  status: string;
  request_id: string;
  count: number;
  next_cursor?: string;
}

export interface MassiveTickerDetail {
  ticker: string;
  name: string;
  market: string;
  locale: string;
  primary_exchange: string;
  type: string;
  active: boolean;
  currency_name: string;
  market_cap: number;
  phone_number?: string;
  address?: {
    address1: string;
    city: string;
    state: string;
    postal_code: string;
  };
  description?: string;
  sic_code?: string;
  sic_description?: string;
  homepage_url?: string;
  total_employees?: number;
  list_date?: string;
  branding?: {
    logo_url?: string;
    icon_url?: string;
  };
  share_class_shares_outstanding?: number;
  weighted_shares_outstanding?: number;
}

export interface MassiveTickerDetailResponse {
  results: MassiveTickerDetail;
  status: string;
  request_id: string;
}

export interface MassiveAggregate {
  v: number;   // volume
  vw: number;  // volume-weighted avg price
  o: number;   // open
  c: number;   // close
  h: number;   // high
  l: number;   // low
  t: number;   // timestamp (ms)
  n: number;   // number of transactions
}

export interface MassiveAggregatesResponse {
  ticker: string;
  adjusted: boolean;
  queryCount: number;
  resultsCount: number;
  status: string;
  request_id: string;
  results: MassiveAggregate[];
}

export interface MassiveSnapshotTicker {
  ticker: string;
  todaysChangePerc: number;
  todaysChange: number;
  updated: number;
  day: { o: number; h: number; l: number; c: number; v: number; vw: number };
  lastTrade: { p: number; s: number; t: number; x: number };
  lastQuote: { P: number; S: number; p: number; s: number; t: number };
  min: { av: number; t: number; n: number; o: number; h: number; l: number; c: number; v: number; vw: number };
  prevDay: { o: number; h: number; l: number; c: number; v: number; vw: number };
}

export interface MassiveSnapshotResponse {
  status: string;
  tickers: MassiveSnapshotTicker[];
  request_id?: string;
}

export interface MassivePreviousCloseResponse {
  ticker: string;
  queryCount: number;
  resultsCount: number;
  adjusted: boolean;
  results: MassiveAggregate[];
  status: string;
  request_id: string;
}

// ─── Service ──────────────────────────────────────────────────────────────────

@Injectable({
  providedIn: 'root'
})
export class MassiveApiService {

  private baseUrl = environment.massiveApiUrl;
  private apiKey = environment.massiveApiKey;

  constructor(private http: HttpClient) {}

  /** Build auth headers for Massive API */
  private headers(): HttpHeaders {
    return new HttpHeaders({
      'Authorization': `Bearer ${this.apiKey}`,
      'Content-Type': 'application/json'
    });
  }

  /**
   * Search / list tickers
   * GET /v3/reference/tickers
   * @param search  partial company name or ticker symbol
   * @param limit   max results (default 10)
   */
  searchTickers(search: string, limit: number = 10): Observable<MassiveTickersResponse> {
    const params = new HttpParams()
      .set('search', search)
      .set('active', 'true')
      .set('limit', String(limit));

    return this.http.get<MassiveTickersResponse>(
      `${this.baseUrl}/v3/reference/tickers`,
      { headers: this.headers(), params }
    ).pipe(
      catchError(err => {
        console.error('[MassiveAPI] searchTickers error:', err);
        return of({ results: [], status: 'ERROR', request_id: '', count: 0 });
      })
    );
  }

  /**
   * Get detailed info for a single ticker
   * GET /v3/reference/tickers/{tickerSymbol}
   */
  getTickerDetails(ticker: string): Observable<MassiveTickerDetailResponse> {
    return this.http.get<MassiveTickerDetailResponse>(
      `${this.baseUrl}/v3/reference/tickers/${ticker.toUpperCase()}`,
      { headers: this.headers() }
    ).pipe(
      catchError(err => {
        console.error(`[MassiveAPI] getTickerDetails(${ticker}) error:`, err);
        return of(null);
      })
    );
  }

  /**
   * Get OHLCV aggregates (candlestick / historical data)
   * GET /v2/aggs/ticker/{ticker}/range/{multiplier}/{timespan}/{from}/{to}
   * @param ticker     e.g. 'AAPL'
   * @param multiplier e.g. 1
   * @param timespan   'minute' | 'hour' | 'day' | 'week' | 'month' | 'quarter' | 'year'
   * @param from       'YYYY-MM-DD'
   * @param to         'YYYY-MM-DD'
   * @param adjusted   adjust for splits (default true)
   */
  getAggregates(
    ticker: string,
    multiplier: number = 1,
    timespan: string = 'day',
    from: string,
    to: string,
    adjusted: boolean = true
  ): Observable<MassiveAggregatesResponse> {
    const params = new HttpParams()
      .set('adjusted', String(adjusted))
      .set('sort', 'asc')
      .set('limit', '5000');

    return this.http.get<MassiveAggregatesResponse>(
      `${this.baseUrl}/v2/aggs/ticker/${ticker.toUpperCase()}/range/${multiplier}/${timespan}/${from}/${to}`,
      { headers: this.headers(), params }
    ).pipe(
      catchError(err => {
        console.error(`[MassiveAPI] getAggregates(${ticker}) error:`, err);
        return of(null);
      })
    );
  }

  /**
   * Get 1-year daily OHLCV history — convenience wrapper
   */
  getOneYearHistory(ticker: string): Observable<MassiveAggregatesResponse> {
    const to = new Date();
    const from = new Date();
    from.setFullYear(from.getFullYear() - 1);
    return this.getAggregates(
      ticker, 1, 'day',
      from.toISOString().split('T')[0],
      to.toISOString().split('T')[0]
    );
  }

  /**
   * Get previous day's close for a ticker
   * GET /v2/aggs/ticker/{ticker}/prev
   */
  getPreviousClose(ticker: string): Observable<MassivePreviousCloseResponse> {
    return this.http.get<MassivePreviousCloseResponse>(
      `${this.baseUrl}/v2/aggs/ticker/${ticker.toUpperCase()}/prev`,
      { headers: this.headers() }
    ).pipe(
      catchError(err => {
        console.error(`[MassiveAPI] getPreviousClose(${ticker}) error:`, err);
        return of(null);
      })
    );
  }

  /**
   * Get real-time snapshot for a single ticker
   * GET /v2/snapshot/locale/us/markets/stocks/tickers/{ticker}
   */
  getSnapshot(ticker: string): Observable<{ status: string; ticker: MassiveSnapshotTicker }> {
    return this.http.get<{ status: string; ticker: MassiveSnapshotTicker }>(
      `${this.baseUrl}/v2/snapshot/locale/us/markets/stocks/tickers/${ticker.toUpperCase()}`,
      { headers: this.headers() }
    ).pipe(
      catchError(err => {
        console.error(`[MassiveAPI] getSnapshot(${ticker}) error:`, err);
        return of(null);
      })
    );
  }

  /**
   * Get snapshots for multiple tickers at once (gainers/losers/movers)
   * GET /v2/snapshot/locale/us/markets/stocks/tickers?tickers=AAPL,GOOG,...
   */
  getSnapshots(tickers: string[]): Observable<MassiveSnapshotResponse> {
    const params = new HttpParams().set('tickers', tickers.map(t => t.toUpperCase()).join(','));

    return this.http.get<MassiveSnapshotResponse>(
      `${this.baseUrl}/v2/snapshot/locale/us/markets/stocks/tickers`,
      { headers: this.headers(), params }
    ).pipe(
      catchError(err => {
        console.error('[MassiveAPI] getSnapshots error:', err);
        return of({ status: 'ERROR', tickers: [] });
      })
    );
  }

  /**
   * Get top gainers or losers
   * GET /v2/snapshot/locale/us/markets/stocks/{direction}
   * @param direction 'gainers' | 'losers'
   */
  getGainersOrLosers(direction: 'gainers' | 'losers'): Observable<MassiveSnapshotResponse> {
    return this.http.get<MassiveSnapshotResponse>(
      `${this.baseUrl}/v2/snapshot/locale/us/markets/stocks/${direction}`,
      { headers: this.headers() }
    ).pipe(
      catchError(err => {
        console.error(`[MassiveAPI] getGainersOrLosers(${direction}) error:`, err);
        return of({ status: 'ERROR', tickers: [] });
      })
    );
  }

  /**
   * Get recent news articles for a ticker
   * GET /v2/reference/news?ticker={ticker}
   */
  getTickerNews(ticker: string, limit: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('ticker', ticker.toUpperCase())
      .set('limit', String(limit))
      .set('sort', 'published_utc')
      .set('order', 'desc');

    return this.http.get<any>(
      `${this.baseUrl}/v2/reference/news`,
      { headers: this.headers(), params }
    ).pipe(
      catchError(err => {
        console.error(`[MassiveAPI] getTickerNews(${ticker}) error:`, err);
        return of({ results: [], status: 'ERROR', count: 0 });
      })
    );
  }

  /**
   * Get last trade (real-time tick)
   * GET /v2/last/trade/{ticker}
   */
  getLastTrade(ticker: string): Observable<any> {
    return this.http.get<any>(
      `${this.baseUrl}/v2/last/trade/${ticker.toUpperCase()}`,
      { headers: this.headers() }
    ).pipe(
      catchError(err => {
        console.error(`[MassiveAPI] getLastTrade(${ticker}) error:`, err);
        return of(null);
      })
    );
  }

  /**
   * Helper: format a price change as a percentage string with sign
   */
  formatChangePercent(value: number): string {
    const sign = value >= 0 ? '+' : '';
    return `${sign}${value.toFixed(2)}%`;
  }

  /**
   * Helper: format timestamp (ms) to locale date string
   */
  formatDate(timestampMs: number): string {
    return new Date(timestampMs).toLocaleDateString('en-IN', {
      day: 'numeric', month: 'short', year: 'numeric'
    });
  }
}
