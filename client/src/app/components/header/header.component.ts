import { BooleanInput } from '@angular/cdk/coercion';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import { Observable } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';
import { SideNavService } from 'src/app/services/side-nav.service';
import { UserService } from 'src/app/services/user.service';
import { LoginModalComponent } from './login-modal/login-modal.component';
import { SignupModalComponent } from './signup-modal/signup-modal.component';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css'],
})
export class HeaderComponent implements OnInit {
  isSideNav: BooleanInput;

  isLogin: Boolean;
  sideNavItem: Array<{ text: string; link: string }>;
  isHandset$: Observable<boolean> = this.breakpointObserver.observe(Breakpoints.Handset).pipe(
    map((result) => result.matches),
    shareReplay(),
  );

  constructor(
    private breakpointObserver: BreakpointObserver,
    private dialog: MatDialog,
    private sideNavService: SideNavService,
    private userService: UserService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.sideNavService.toggleSideNav().subscribe((value) => {
      this.isSideNav = value;
    });
    this.sideNavService.setSideNav().subscribe((value) => {
      this.sideNavItem = value;
    });
    this.userService.isLoggedIn().subscribe((value) => {
      this.isLogin = value;
    });
    this.isLogin = localStorage.getItem('token') != null;
  }

  onLogin() {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.autoFocus = true;
    dialogConfig.width = '35%';
    this.dialog.open(LoginModalComponent, dialogConfig);
  }

  onSignUp() {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.autoFocus = true;
    dialogConfig.width = '60%';
    this.dialog.open(SignupModalComponent, dialogConfig);
  }

  onLogOut() {
    this.userService.logout();
    this.toastr.success('Log Out Successfull!', '', {
      closeButton: true,
      positionClass: 'toast-bottom-right',
    });
    window.location.reload();
  }
}
