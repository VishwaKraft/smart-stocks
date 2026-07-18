import { Component, HostListener, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { EventTrackingService } from './services/event-tracking.service';
import { NgwWowService } from 'ngx-wow';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  title = 'smart-stocks';

  constructor(
    private router: Router,
    private eventTracking: EventTrackingService,
    private wowService: NgwWowService
  ) {
    this.wowService.init();
  }

  ngOnInit() {
    if (window) {
      window.console.log = function () { };
      window.console.error = function () { };
    }
    this.eventTracking.init();
    if (localStorage.getItem('token')) {
      if (this.router.url === '/login' || this.router.url === '/register' || this.router.url === '/') {
        this.router.navigate(['/dashboard/home']);
      }
    } else {
      if (this.router.url !== '/login' && this.router.url !== '/register' && this.router.url !== '/home') {
        this.router.navigate(['/home']);
      }
    }
  }


}
