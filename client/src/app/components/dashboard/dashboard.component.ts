import { Component, OnInit } from '@angular/core';
import { SideNavService } from 'src/app/services/side-nav.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
})
export class DashboardComponent implements OnInit {
  constructor(private sideNavService: SideNavService) {}

  ngOnInit(): void {
    window.scrollTo(0, 0);
  }
}
