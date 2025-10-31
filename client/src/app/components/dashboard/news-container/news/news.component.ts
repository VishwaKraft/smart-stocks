import { Component, OnInit, Input, OnChanges, SimpleChanges } from '@angular/core';
import { NewsBody } from 'src/app/Interface/NewsBody';
import { NewsService } from 'src/app/services/news.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-news',
  templateUrl: './news.component.html',
  styleUrls: ['./news.component.css']
})
export class NewsComponent implements OnInit, OnChanges {

  requireLoader: boolean = false;
  latestNews: NewsBody[];
  index: number = 0;
  maxindex: number;
  errorMessage: string;

  @Input() newsType: string = 'business';

  constructor(private newsService: NewsService, private toastr: ToastrService) { }

  ngOnInit(): void {
    this.loadNews();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['newsType'] && !changes['newsType'].firstChange) {
      this.loadNews();
    }
  }

  loadNews(): void {
    this.requireLoader = true;
    this.errorMessage = '';
    this.index = 0;
    
    this.newsService
      .getLatestStockNews(this.newsType)
      .subscribe(
        value => {
          if (value.data && value.data.length > 0) {
            this.latestNews = value.data;
            this.maxindex = value.data.length;
          } else {
            this.latestNews = [];
            this.maxindex = 0;
            this.errorMessage = 'No news available for this category.';
          }
          this.requireLoader = false;
        },
        error => {
          console.error('Error loading news:', error);
          this.requireLoader = false;
          this.latestNews = [];
          this.maxindex = 0;
          
          if (error.error && error.error.errors && error.error.errors.error) {
            this.errorMessage = error.error.errors.error;
          } else {
            this.errorMessage = 'Failed to load news. Please try again later.';
          }
          
          this.toastr.error(this.errorMessage, 'News Error');
        }
      );
  }

  nextNews() {
    if (this.index < this.maxindex - 1) {
      this.index += 1;
    }
  }

  prevNews() {
    if (this.index > 0) {
      this.index -= 1;
    }
  }
}
