import { Component, OnDestroy, OnInit } from '@angular/core';
import { SideNavService } from 'src/app/services/side-nav.service';

@Component({
  selector: 'app-sandbox',
  templateUrl: './sandbox.component.html',
  styleUrls: ['./sandbox.component.css']
})
export class SandboxComponent implements OnInit, OnDestroy {
  constructor(private sideNavService: SideNavService) {

  }

  ngOnDestroy(): void {
    this.sideNavService.sideNav = false;
    this.sideNavService.sideNavSubject.next(this.sideNavService.sideNav);
    this.sideNavService.sideNavItems = [];
    this.sideNavService.sideNavItemsSubject.next(this.sideNavService.sideNavItems);
  }

  ngOnInit(): void {
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
