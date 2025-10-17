import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class UiService {
  public discoverStockEndpoint: string = 'gainer';

  discoverSubject = new Subject<any>();

  getDiscoverSubscription(): Observable<any> {
    this.discoverSubject.next(this.discoverStockEndpoint);
    return this.discoverSubject.asObservable();
  }

  constructor() {}
}
