import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/distinctUntilChanged';
import 'rxjs/add/operator/switchMap';
import { debounceTime } from 'rxjs/operators';
import { switchMap } from 'rxjs/operators';
import { map } from 'rxjs/operators';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root',
})
export class SearchService {
  baseUrl = `${environment.serverUrl}/stock/search`;
  queryUrl: string = '?q=';

  constructor(private http: HttpClient) {}

  search(terms: Observable<string>) {
    return terms.pipe(debounceTime(400)).pipe(switchMap((term) => this.searchEntries(term)));
  }

  searchEntries(term: string) {
    return this.http
      .get<Response>(this.baseUrl + this.queryUrl + term + '&limit=5')
      .pipe(map((res) => res));
  }
}
