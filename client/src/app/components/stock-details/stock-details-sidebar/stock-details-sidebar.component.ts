import { Component, Inject, OnInit } from '@angular/core';
import { StockDetailsModel } from 'src/app/models/stockDetailsModel';
import { StockDetailsService } from 'src/app/services/stock-details.service';
import { ActivatedRoute } from '@angular/router';
import {
  MatDialog,
  MatDialogConfig,
  MatDialogRef,
  MAT_DIALOG_DATA,
} from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-stock-details-sidebar',
  templateUrl: './stock-details-sidebar.component.html',
  styleUrls: ['./stock-details-sidebar.component.css'],
})
export class StockDetailsSidebarComponent implements OnInit {
  changeLow: boolean;
  stockDetailsService: StockDetailsService;
  wishlist: boolean = true;
  noOfUnits: any;
  durationInSeconds = 3;
  wishlistImage: string = this.wishlist ? 'assets/heartFilled.svg' : 'assets/heartEmpty.svg';
  urlStockDetails: StockDetailsModel = new StockDetailsModel(
    '',
    '',
    '',
    '',
    '',
    '',
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    { fmt: '' },
    '',
    { fmt: '' },
    '',
    '',
    { fmt: '' },
  );
  symbol: string = '';
  activatedRoute: ActivatedRoute;
  id: any;
  constructor(
    stockDetailsService: StockDetailsService,
    activatedRoute: ActivatedRoute,
    public dialog: MatDialog,
    private _snackBar: MatSnackBar,
  ) {
    this.stockDetailsService = stockDetailsService;
    this.activatedRoute = activatedRoute;
  }

  ngOnInit(): void {
    this.symbol = this.activatedRoute.snapshot.params['symbol'];

    if (this.symbol != '') {
      this.stockDetailsService.fetchStockDetails(this.symbol).subscribe((res) => {
        this.urlStockDetails = res.data;
        this.stockDetailsService.setStockDetails(this.urlStockDetails);
        this.changeLow =
          this.urlStockDetails.change.fmt.toString().charAt(0) === '-' ? true : false;
      });
    }

    this.id = setInterval(() => {
      this.stockDetailsService.fetchStockDetails(this.symbol).subscribe((res) => {
        this.urlStockDetails = res.data;
        this.stockDetailsService.setStockDetails(this.urlStockDetails);
        this.changeLow =
          this.urlStockDetails.change.fmt.toString().charAt(0) === '-' ? true : false;
      });
    }, 3000);
  }

  ngOnDestroy() {
    if (this.id) {
      clearInterval(this.id);
    }
  }

  openDialog(): void {
    const dialogConfig = new MatDialogConfig();
    dialogConfig.autoFocus = true;
    this.dialog.open(DialogOverviewExampleDialogComponent, dialogConfig);
  }

  openSnackBar() {
    this._snackBar.openFromComponent(BuyComponent, {
      duration: this.durationInSeconds * 1000,
    });
  }
}

@Component({
  selector: 'dialog-overview-example-dialog',
  templateUrl: 'dialog-overview-example-dialog.html',
  styleUrls: ['dialog-overview-example-dialog.css'],
})
export class DialogOverviewExampleDialogComponent implements OnInit {
  noOfUnits: any;
  curPrice: string;
  boughtPrice: number;
  profit_loss: string;
  stockDetailsService: StockDetailsService;
  constructor(
    public dialogRef: MatDialogRef<DialogOverviewExampleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: number,
    stockDetailsService: StockDetailsService,
  ) {
    this.stockDetailsService = stockDetailsService;
  }

  ngOnInit() {
    this.curPrice = this.stockDetailsService.getCurrentPrice();
  }

  onNoClick(): void {
    this.dialogRef.close();
  }

  calculateAmount() {
    var numCurPrice = this.curPrice.split(',').join('').toString();
    this.profit_loss = (
      parseFloat(numCurPrice) * this.noOfUnits -
      this.boughtPrice * this.noOfUnits
    ).toFixed(2);
  }
}

@Component({
  selector: 'buy-component',
  template: `<span class="example-pizza-party"> Stock has been added to watchlist!! </span>`,

  styles: [
    `
      .example-pizza-party {
        color: white;
      }
    `,
  ],
})
export class BuyComponent {}
