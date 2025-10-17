import { Injectable, Injector } from '@angular/core';
import { HttpInterceptor } from '@angular/common/http';
import { UserService } from './user.service';
@Injectable()
export class TokenInterceptorService implements HttpInterceptor {
  constructor(private injector: Injector) {}
  intercept(req, next) {
    let authService = this.injector.get(UserService);
    let tokenizedReq = req.clone({
      headers:
        authService.getToken() !== null
          ? req.headers.set('Authorization', authService.getToken())
          : '',
    });
    return next.handle(tokenizedReq);
  }
}

