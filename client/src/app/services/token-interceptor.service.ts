import { Injectable, Injector } from '@angular/core';
import { HttpInterceptor } from '@angular/common/http';
import { UserService } from './user.service';
@Injectable()
export class TokenInterceptorService implements HttpInterceptor {
  constructor(private injector: Injector) {}
  intercept(req, next) {
    const authService = this.injector.get(UserService);
    const tokenizedReq = req.clone({
      headers:
        authService.getToken() !== null
          ? req.headers.set('Authorization', authService.getToken())
          : '',
    });
    return next.handle(tokenizedReq);
  }
}
