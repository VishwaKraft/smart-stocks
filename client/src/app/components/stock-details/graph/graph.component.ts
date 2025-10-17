import { Component, OnInit } from '@angular/core';
import * as CanvasJS from 'src/resources/canvasjs/dist/canvasjs.stock.min';
import { ActivatedRoute, Router } from '@angular/router';
import { StockDetailsService } from 'src/app/services/stock-details.service';
import { Subscription } from 'rxjs';
import { GraphData } from 'src/app/Interface/GraphData';

@Component({
  selector: 'app-graph',
  templateUrl: './graph.component.html',
  styleUrls: ['./graph.component.css'],
})
export class GraphComponent implements OnInit {
  symbol: string = '';
  urlData: GraphData[];
  dataPoints1 = [];
  dataPoints2 = [];
  dataPoints3 = [];
  displayLine = true;
  graphTypeLine: boolean = true;
  dataSubscription: Subscription;
  graphSubscription: Subscription;
  stockName;
  stockChart;
  priceTrendUp: boolean = true;
  zoomOut: Boolean = true;
  candleStickConfig = [
    {
      name: 'Price',
      yValueFormatString: '₹#,###.##',
      xValueFormatString: 'MMM DD YYYY',
      type: 'candlestick',
      color: 'orange',
      risingColor: 'green',
      fallingColor: '#ff1212',
      dataPoints: this.dataPoints1,
    },
  ];
  lineConfig = [
    {
      name: 'Price',
      yValueFormatString: '₹#,###.##',
      xValueFormatString: 'MMM DD YYYY',
      type: 'line',
      color: this.priceTrendUp ? 'green' : 'red',
      dataPoints: this.dataPoints3,
    },
  ];
  router: any;

  constructor(
    private activatedRoute: ActivatedRoute,
    private stockDetailsService: StockDetailsService,
    private _router: Router,
  ) {}

  addSymbols(e) {
    var suffixes = ['', 'K', 'M', 'B'];
    var order = Math.max(Math.floor(Math.log(e.value) / Math.log(1000)), 0);
    if (order > suffixes.length - 1) order = suffixes.length - 1;
    var suffix = suffixes[order];
    return CanvasJS.formatNumber(e.value / Math.pow(1000, order)) + suffix;
  }

  ngOnInit() {
    this.router = this._router.url;
    this.symbol = this.activatedRoute.snapshot.params['symbol'];
    this.priceTrendUp = this.stockDetailsService.getPriceTrend();
    if (this.router == '/graph/' + this.symbol) {
      this.zoomOut = false;
    } else if (this.router == '/stockDetails/' + this.symbol) {
      this.zoomOut = true;
    }
    this.stockDetailsService.fetchGraphData(this.symbol).subscribe((res) => {
      this.urlData = res.data;
      this.priceTrendUp = this.stockDetailsService.getPriceTrend();
      this.stockChart = new CanvasJS.StockChart('chartContainer', {
        exportEnabled: false,
        theme: 'light2',
        title: {
          horizontalAlign: 'left',
          text: this.stockName,
          fontFamily: "'Poppins', sans-serif",
        },
        charts: [
          {
            zoomEnabled: true,
            toolTip: {
              shared: true,
            },
            axisX: {
              lineThickness: 0,
              tickLength: 0,
              labelFormatter: function (e) {
                return '';
              },
              crosshair: {
                enabled: true,
                snapToDataPoint: true,
                labelFormatter: function (e) {
                  return '';
                },
              },
            },
            axisY: {
              lineThickness: 0,
              tickLength: 0,
              labelFormatter: function (e) {
                return '';
              },
            },
            legend: {
              verticalAlign: 'top',
            },
            data: this.graphTypeLine ? this.lineConfig : this.candleStickConfig,
          },
          {
            height: 130,
            toolTip: {
              shared: true,
            },
            axisX: {
              lineThickness: 0,
              tickLength: 0,
              labelFormatter: function (e) {
                return '';
              },
              crosshair: {
                enabled: true,
              },
            },
            axisY: {
              lineThickness: 0,
              tickLength: 0,
              labelFormatter: function (e) {
                return '';
              },
            },
            legend: {
              horizontalAlign: 'left',
            },
            data: [
              {
                name: 'Volume',
                yValueFormatString: '₹#,###.##',
                xValueFormatString: 'MMM DD YYYY',
                dataPoints: this.dataPoints2,
              },
            ],
          },
        ],

        rangeSelector: {
          verticalAlign: 'bottom',
          horizontalAlign: 'left',
          label: ' ',
          inputFields: {
            style: {
              padding: { left: 10, right: 10, top: 3, bottom: 3 },
            },
          },
          buttonStyle: {
            spacing: 15,
            with: 30,
            maxWidth: 50,
            padding: { left: 10, right: 10, top: 3, bottom: 3 },
          },
          buttons: [
            {
              range: 1,
              rangeType: 'day',
              label: '1d',
            },
            {
              range: 1,
              rangeType: 'week',
              label: '1w',
            },
            {
              range: 1,
              rangeType: 'month',
              label: '1m',
            },
            {
              range: 3,
              rangeType: 'month',
              label: '3m',
            },
            {
              range: 6,
              rangeType: 'month',
              label: '6m',
            },
            {
              range: 1,
              rangeType: 'ytd',
              label: 'ytd',
            },
            {
              range: 1,
              rangeType: 'year',
              label: '1y',
            },
            {
              rangeType: 'all',
              label: 'All',
            },
          ],
        },
        navigator: {
          enabled: false,
        },
      });
      let data = this.urlData;
      for (var i = 0; i < data.length; i++) {
        if (this.urlData[i].open === 0) continue;
        let curDate = new Date(this.urlData[i].date * 1000);
        let dateFormatValue =
          curDate.getFullYear() + '-' + curDate.getMonth() + '-' + curDate.getDate();
        this.dataPoints1.push({
          x: curDate,
          y: [
            Number(data[i].open),
            Number(data[i].high),
            Number(data[i].low),
            Number(data[i].close),
          ],
          color: data[i].open < data[i].close ? 'green' : 'red',
        });
        this.dataPoints2.push({
          x: curDate,
          y: Number(data[i].volume),
          color: data[i].open < data[i].close ? 'green' : 'red',
        });
        this.dataPoints3.push({ x: curDate, y: Number(data[i].close) });
      }
      this.stockChart.render();
    });
    this.dataSubscription = this.stockDetailsService.dataChanged.subscribe(() => {
      this.stockName = this.stockDetailsService.getStockLongName();
      this.priceTrendUp = this.stockDetailsService.getPriceTrend();
    });

    this.graphSubscription = this.stockDetailsService.graphChanged.subscribe(() => {
      this.graphTypeLine = this.stockDetailsService.getGraphType() === 'line';
      this.priceTrendUp = this.stockDetailsService.getPriceTrend();
      this.lineConfig[0].color = this.priceTrendUp ? 'green' : 'red';
      this.stockChart = new CanvasJS.StockChart('chartContainer', {
        exportEnabled: false,
        theme: 'light2',
        title: {
          horizontalAlign: 'left',
          text: this.stockName,
          fontFamily: "'Poppins', sans-serif",
        },
        charts: [
          {
            zoomEnabled: true,
            toolTip: {
              shared: true,
            },
            axisX: {
              lineThickness: 0,
              tickLength: 0,
              labelFormatter: function (e) {
                return '';
              },
              crosshair: {
                enabled: true,
                snapToDataPoint: true,
                labelFormatter: function (e) {
                  return '';
                },
              },
            },
            axisY: {
              lineThickness: 0,
              tickLength: 0,
              labelFormatter: function (e) {
                return '';
              },
            },
            legend: {
              verticalAlign: 'top',
            },
            data: this.graphTypeLine ? this.lineConfig : this.candleStickConfig,
          },
          {
            height: 130,
            toolTip: {
              shared: true,
            },
            axisX: {
              lineThickness: 0,
              tickLength: 0,
              labelFormatter: function (e) {
                return '';
              },
              crosshair: {
                enabled: true,
              },
            },
            axisY: {
              lineThickness: 0,
              tickLength: 0,
              labelFormatter: function (e) {
                return '';
              },
            },
            legend: {
              horizontalAlign: 'left',
            },
            data: [
              {
                name: 'Volume',
                yValueFormatString: '₹#,###.##',
                xValueFormatString: 'MMM DD YYYY',
                dataPoints: this.dataPoints2,
              },
            ],
          },
        ],

        rangeSelector: {
          verticalAlign: 'bottom',
          horizontalAlign: 'left',
          label: ' ',
          inputFields: {
            style: {
              padding: { left: 10, right: 10, top: 3, bottom: 3 },
            },
          },
          buttonStyle: {
            spacing: 15,
            with: 30,
            maxWidth: 50,
            padding: { left: 10, right: 10, top: 3, bottom: 3 },
          },
          buttons: [
            {
              range: 1,
              rangeType: 'day',
              label: '1d',
            },
            {
              range: 1,
              rangeType: 'week',
              label: '1w',
            },
            {
              range: 1,
              rangeType: 'month',
              label: '1m',
            },
            {
              range: 3,
              rangeType: 'month',
              label: '3m',
            },
            {
              range: 6,
              rangeType: 'month',
              label: '6m',
            },
            {
              range: 1,
              rangeType: 'ytd',
              label: 'ytd',
            },
            {
              range: 1,
              rangeType: 'year',
              label: '1y',
            },
            {
              rangeType: 'all',
              label: 'All',
            },
          ],
        },
        navigator: {
          enabled: false,
        },
      });

      this.stockChart.render();
    });
  }

  displayLineGraph() {
    this.stockDetailsService.setGraphType('line');
  }

  displayCandleGraph() {
    this.stockDetailsService.setGraphType('candle');
  }

  displayGraph() {
    console.log('zoom');
  }

  handleZoomIn() {
    this.zoomOut = !this.zoomOut;
    this._router.navigateByUrl('/graph/' + this.symbol);
  }

  handleZoomOut() {
    this.zoomOut = !this.zoomOut;
    this._router.navigateByUrl('/stockDetails/' + this.symbol);
  }

  ngOnDestroy() {
    this.dataSubscription.unsubscribe();
    this.graphSubscription.unsubscribe();
  }
}
