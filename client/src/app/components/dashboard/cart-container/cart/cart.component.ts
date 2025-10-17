import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-cart',
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css'],
})
export class CartComponent implements OnInit {
  @Input() iconName: string = '';
  @Input() iconText: string = '';
  @Input() iconHeading: string = '';

  constructor() {}

  ngOnInit(): void {}
}
