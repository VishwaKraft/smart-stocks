import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import { SideNavService } from 'src/app/services/side-nav.service';
import { LoginModalComponent } from '../../header/login-modal/login-modal.component';

@Component({
  selector: 'app-sandbox',
  templateUrl: './sandbox.component.html',
  styleUrls: ['./sandbox.component.css']
})
export class SandboxComponent implements OnInit, OnDestroy {
  constructor(
    private sideNavService: SideNavService,
    private router: Router,
    private dialog: MatDialog,
    private toastr: ToastrService
  ) {

  }

  ngOnDestroy(): void {
    this.sideNavService.sideNav = false;
    this.sideNavService.sideNavSubject.next(this.sideNavService.sideNav);
    this.sideNavService.sideNavItems = [];
    this.sideNavService.sideNavItemsSubject.next(this.sideNavService.sideNavItems);
  }

  ngOnInit(): void {
    // Auth Check
    const token = localStorage.getItem('token');
    if (!token) {
      this.toastr.warning("Please login to access the Sandbox", "Unauthorized", {
        closeButton: true,
        positionClass: "toast-bottom-right",
      });
      
      const dialogConfig = new MatDialogConfig();
      dialogConfig.autoFocus = true;
      this.dialog.open(LoginModalComponent, dialogConfig);
      
      this.router.navigate(['/university']);
      return;
    }

    this.sideNavService.sideNav = true;
    this.sideNavService.sideNavSubject.next(this.sideNavService.sideNav);
    this.sideNavService.sideNavItems = [{
      text: "Inventory Overview",
      link: "/user/sandbox/dashboard"
    },
    {
      text: "Stock Movements",
      link: "/user/sandbox/orders"
    },
    {
      text: "Budget History",
      link: "/user/sandbox/credits"
    },
    {
      text: "Product Catalog",
      link: "/user/sandbox/stocks"
    }];
    this.sideNavService.sideNavItemsSubject.next(this.sideNavService.sideNavItems);
    this.sideNavService.sideNavSubject.next(this.sideNavService.sideNav);
  }

}
