import { Injectable } from '@angular/core';
import { Demo } from '../Interface/demo';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DemoR } from '../Interface/demoR';

@Injectable({
  providedIn: 'root',
})
export class DemoService {
  baseUrl: string = 'https://api.cdnjs.com/libraries';
  public demoLinks: Demo[] = [];

  constructor(private http: HttpClient) {}

  getDemoData(): Observable<DemoR> {
    return this.http.get<DemoR>(this.baseUrl);
  }
}
