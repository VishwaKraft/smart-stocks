import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { Demo } from 'src/app/Interface/demo';
import { DemoService } from 'src/app/services/demo.service';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { UiService } from 'src/app/services/ui.service';
import { SideNavService } from 'src/app/services/side-nav.service';
import { NGXLogger } from 'ngx-logger';

@Component({
  selector: 'app-watchlist',
  templateUrl: './watchlist.component.html',
  styleUrls: ['./watchlist.component.scss'],
})
export class WatchlistComponent implements OnInit {
  public isMenuOpen: boolean = true;
  public selectedVal: string;
  endpoint: string = window.location.href.split('=')[1];
  demoData: MatTableDataSource<Demo>;
  displayedColumns: string[] = ['name', 'latest'];
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  constructor(
    private demoService: DemoService,
    private uiService: UiService,
    private sideNavService: SideNavService,
    private logger: NGXLogger,
  ) {}

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.sideNavService.sideNav = true;
    this.sideNavService.sideNavSubject.next(this.sideNavService.sideNav);
    this.sideNavService.sideNavItems = [
      {
        text: 'Top Gainer',
        link: '/discover/top-gainer',
      },
      {
        text: 'Top Loser',
        link: '/discover/top-loser',
      },
      {
        text: 'Top Recommendation',
        link: '/discover/recommendation',
      },
      {
        text: 'Best of this Month',
        link: '/discover/best-month',
      },
      {
        text: 'All Stocks',
        link: '/discover/all-stocks',
      },
    ];
    this.sideNavService.sideNavItemsSubject.next(this.sideNavService.sideNavItems);
    this.selectedVal = this.uiService.discoverStockEndpoint;
    this.demoService.getDemoData().subscribe((value) => {
      this.demoData = new MatTableDataSource(value.results);
      this.demoData.paginator = this.paginator;
      this.demoData.sort = this.sort;
      this.logger.debug(this.demoData);
    });
  }

  ngOnDestroy() {
    this.sideNavService.sideNav = false;
    this.sideNavService.sideNavSubject.next(this.sideNavService.sideNav);
    this.sideNavService.sideNavItems = [];
    this.sideNavService.sideNavItemsSubject.next(this.sideNavService.sideNavItems);
  }

  ngAfterViewInit() {
    this.demoData.paginator = this.paginator;
    this.demoData.sort = this.sort;
  }

  public onSidenavClick(): void {
    this.isMenuOpen = false;
  }

  contentHandler(val: string) {
    this.selectedVal = val;
  }

  applyFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.demoData.filter = filterValue.trim().toLowerCase();

    if (this.demoData.paginator) {
      this.demoData.paginator.firstPage();
    }
  }
}
