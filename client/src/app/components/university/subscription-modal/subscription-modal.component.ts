import { Component, OnInit } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-subscription-modal',
  templateUrl: './subscription-modal.component.html',
  styleUrls: ['./subscription-modal.component.css'],
})
export class SubscriptionModalComponent implements OnInit {
  constructor(public dialogRef: MatDialogRef<SubscriptionModalComponent>) {}

  ngOnInit(): void {}

  handleOk() {
    this.dialogRef.close();
  }
}
