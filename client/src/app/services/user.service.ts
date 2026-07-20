import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  public isLogin: boolean;
  isLoginSubject = new Subject<any>();
  constructor(private http: HttpClient) {
    this.isLogin = (localStorage.getItem('token') != null)
  }

  // Checking if token is set
  isLoggedIn(): Observable<any> {
    this.isLoginSubject.next(localStorage.getItem('token') != null);
    return this.isLoginSubject.asObservable();
  }

  // Checking if token is set
  loggedIn(): Boolean {
    return !!localStorage.getItem('token');
  }

  // After clearing localStorage redirect to login screen
  logout() {
    localStorage.removeItem('token');
    this.isLoginSubject.next(localStorage.getItem('token') != null);
  }

  getToken() {
    return localStorage.getItem('token') ? localStorage.getItem('token') : null;
  }

  login(user: { email: string, password: string }): Observable<any> {
    const headers = { 'content-type': 'application/json' }
    const body = JSON.stringify(user);
    console.log(body)
    return this.http.post(`${environment.serverUrl}/user/token`, body, { 'headers': headers })
  }

  googleLogin(token: string): Observable<any> {
    const headers = { 'content-type': 'application/json' }
    const body = JSON.stringify({ token });
    return this.http.post(`${environment.serverUrl}/user/google-login`, body, { 'headers': headers })
  }

  getUser(): Observable<any> {
    return this.http.get(`${environment.serverUrl}/user/profile`)
  }

  updateUser(user): Observable<any> {
    const headers = { 'content-type': 'application/json' }
    const body = JSON.stringify(user);
    return this.http.put(`${environment.serverUrl}/user/profile`, body, { 'headers': headers })
  }

  signup(user): Observable<any> {
    const headers = { 'content-type': 'application/json' }
    const body = JSON.stringify(user);
    console.log(body)
    return this.http.post(`${environment.serverUrl}/user/signup`, body, { 'headers': headers })
  }
}
