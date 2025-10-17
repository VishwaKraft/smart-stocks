import { Component, OnInit } from '@angular/core';
import { NewsBody } from 'src/app/Interface/NewsBody';
import { NewsService } from 'src/app/services/news.service';

@Component({
  selector: 'app-news',
  templateUrl: './news.component.html',
  styleUrls: ['./news.component.css'],
})
export class NewsComponent implements OnInit {
  requireLoader: boolean = true;
  latestNews: NewsBody[];
  index: number = 0;
  maxindex: number;

  constructor(private newsService: NewsService) {}

  ngOnInit(): void {
    this.newsService.getLatestStockNews().subscribe((value) => {
      this.latestNews = value.data;
      this.maxindex = value.data.length;
      this.requireLoader = false;
    });
  }

  nextNews() {
    this.index += 1;
  }
  prevNews() {
    this.index -= 1;
  }
}
