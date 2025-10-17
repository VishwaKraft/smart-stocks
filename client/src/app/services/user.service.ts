import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { environment } from 'src/environments/environment';
import { NGXLogger } from 'ngx-logger';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  public isLogin: boolean;
  isLoginSubject = new Subject<any>();
  constructor(
    private http: HttpClient,
    private logger: NGXLogger,
  ) {
    this.isLogin = localStorage.getItem('token') != null;
  }

  isLoggedIn(): Observable<any> {
    this.isLoginSubject.next(localStorage.getItem('token') != null);
    return this.isLoginSubject.asObservable();
  }

  loggedIn(): Boolean {
    return !!localStorage.getItem('token');
  }

  logout() {
    localStorage.removeItem('token');
    this.logger.info('User successfully logged out.');
    this.isLoginSubject.next(localStorage.getItem('token') != null);
  }

  getToken() {
    return localStorage.getItem('token') ? localStorage.getItem('token') : null;
  }

  login(user: { email: string; password: string }): Observable<any> {
    const headers = { 'content-type': 'application/json' };
    const body = JSON.stringify(user);
    this.logger.info(`Attempting login for user: ${user.email}`);
    return this.http.post(`${environment.serverUrl}/user/token`, body, { headers: headers });
  }

  getUser(): Observable<any> {
    return this.http.get(`${environment.serverUrl}/user/profile`);
  }

  updateUser(user): Observable<any> {
    const headers = { 'content-type': 'application/json' };
    const body = JSON.stringify(user);
    return this.http.put(`${environment.serverUrl}/user/profile`, body, { headers: headers });
  }

  signup(user): Observable<any> {
    const headers = { 'content-type': 'application/json' };
    const body = JSON.stringify(user);
    this.logger.info('New user signup request initiated.');
    return this.http.post(`${environment.serverUrl}/user/signup`, body, { headers: headers });
  }
}
