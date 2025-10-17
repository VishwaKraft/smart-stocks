import { Component } from '@angular/core';

import { NgwWowService } from 'ngx-wow';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent {
  title = 'smart-stocks';

  constructor(private wowService: NgwWowService) {
    this.wowService.init();
  }

  ngOnInit(): void {
    if (window) {
      window.console.log = function () {};
      window.console.error = function () {};
    }
  }
}
