import { Injectable, Injector } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler } from '@angular/common/http';
import { UserService } from './user.service';
import { environment } from 'src/environments/environment';

@Injectable()
export class TokenInterceptorService implements HttpInterceptor {

  constructor(private injector: Injector) { }

  intercept(req: HttpRequest<any>, next: HttpHandler) {
    // Only attach JWT auth for our own backend — NOT for Massive API or other external services
    const isOwnBackend = req.url.startsWith(environment.serverUrl) || req.url.startsWith(environment.modelUrl);

    if (isOwnBackend) {
      const authService = this.injector.get(UserService);
      const token = authService.getToken();
      if (token) {
        const tokenizedReq = req.clone({
          headers: req.headers.set('Authorization', token)
        });
        return next.handle(tokenizedReq);
      }
    }

    return next.handle(req);
  }
}