import { Component, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { SearchResponse } from 'src/app/Interface/SearchResponse';
import { SearchService } from 'src/app/services/search.service';
import { NGXLogger } from 'ngx-logger';

@Component({
  selector: 'app-discover',
  templateUrl: './discover.component.html',
  styleUrls: ['./discover.component.css'],
})
export class DiscoverComponent implements OnInit {
  results: SearchResponse[] = [];
  searchTerm$ = new Subject<string>();

  constructor(
    private searchService: SearchService,
    private logger: NGXLogger,
  ) {
    this.searchService.search(this.searchTerm$).subscribe((results: any) => {
      this.logger.debug('Search results received:', results);
      this.results = results.data;
    });
  }

  makeSubject(event: any) {
    this.logger.debug('Search term changed:', event.target.value);
    this.searchTerm$.next(event.target.value);
  }

  ngOnInit(): void {}
}
