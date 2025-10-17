import { Component, OnDestroy, OnInit } from '@angular/core';
import { SideNavService } from 'src/app/services/side-nav.service';

@Component({
  selector: 'app-sandbox',
  templateUrl: './sandbox.component.html',
  styleUrls: ['./sandbox.component.css'],
})
export class SandboxComponent implements OnInit, OnDestroy {
  constructor(private sideNavService: SideNavService) {}

  ngOnDestroy(): void {
    this.sideNavService.sideNav = false;
    this.sideNavService.sideNavSubject.next(this.sideNavService.sideNav);
    this.sideNavService.sideNavItems = [];
    this.sideNavService.sideNavItemsSubject.next(this.sideNavService.sideNavItems);
  }

  ngOnInit(): void {
    this.sideNavService.sideNav = true;
    this.sideNavService.sideNavSubject.next(this.sideNavService.sideNav);
    this.sideNavService.sideNavItems = [
      {
        text: 'Dashboard',
        link: '/user/sandbox/dashboard',
      },
      {
        text: 'Orders History',
        link: '/user/sandbox/orders',
      },
      {
        text: 'Credits History',
        link: '/user/sandbox/credits',
      },
      {
        text: 'Company',
        link: '/user/sandbox/stocks',
      },
    ];
    this.sideNavService.sideNavItemsSubject.next(this.sideNavService.sideNavItems);
    this.sideNavService.sideNavSubject.next(this.sideNavService.sideNav);
  }
}
