import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { UserService } from './user.service';
import { NGXLogger } from 'ngx-logger';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard implements CanActivate {
  constructor(
    private _authService: UserService,
    private _router: Router,
    private logger: NGXLogger,
  ) {}

  canActivate(): boolean {
    if (this._authService.loggedIn()) {
      this.logger.info('User is authenticated. Access GRANTED to route.');
      return true;
    } else {
      this.logger.warn('User is NOT authenticated. Redirecting to login/home.');
      this._router.navigate(['']);
      return false;
    }
  }
}
