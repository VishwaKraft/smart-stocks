import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class SideNavService {
  public sideNav: boolean = false;
  public sideNavItems: Array<{ text: string; link: string }>;
  sideNavSubject = new Subject<any>();
  sideNavItemsSubject = new Subject<any>();

  constructor() {}

  toggleSideNav(): Observable<any> {
    this.sideNavSubject.next(this.sideNav);
    return this.sideNavSubject.asObservable();
  }

  setSideNav(): Observable<any> {
    this.sideNavItemsSubject.next(this.sideNavItems);
    return this.sideNavItemsSubject.asObservable();
  }
}
