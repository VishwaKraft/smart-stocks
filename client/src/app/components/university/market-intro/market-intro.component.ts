import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-market-intro',
  templateUrl: './market-intro.component.html',
  styleUrls: ['./market-intro.component.css'],
})
export class MarketIntroComponent implements OnInit {
  constructor() {}

  ngOnInit(): void {
    window.scrollTo(0, 0);
  }
}
