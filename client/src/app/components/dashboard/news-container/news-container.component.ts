import { Component, OnInit, OnDestroy } from '@angular/core';
import { NewsService } from 'src/app/services/news.service';
import { NewsType } from 'src/app/Interface/NewsType';

@Component({
  selector: 'app-news-container',
  templateUrl: './news-container.component.html',
  styleUrls: ['./news-container.component.css']
})
export class NewsContainerComponent implements OnInit, OnDestroy {

  newsTypes: NewsType[] = [];
  selectedType: string = 'business';
  currentCategoryIndex: number = 0;
  rotationInterval: any;
  rotationIntervalMs: number = 15000; // Rotate every 15 seconds

  constructor(private newsService: NewsService) { }

  ngOnInit(): void {
    this.loadNewsTypes();
  }

  ngOnDestroy(): void {
    if (this.rotationInterval) {
      clearInterval(this.rotationInterval);
    }
  }

  loadNewsTypes(): void {
    this.newsService.getNewsTypes().subscribe(
      response => {
        if (response.data && response.data.length > 0) {
          // Filter to only business and technology categories
          this.newsTypes = response.data.filter(type => 
            type.type === 'business' || type.type === 'technology'
          );
          this.currentCategoryIndex = Math.floor(Math.random() * this.newsTypes.length);
          this.selectedType = this.newsTypes[this.currentCategoryIndex].type;
          this.startRotation();
        }
      },
      error => {
        console.error('Error loading news types:', error);
      }
    );
  }

  startRotation(): void {
    if (this.rotationInterval) {
      clearInterval(this.rotationInterval);
    }
    
    this.rotationInterval = setInterval(() => {
      this.currentCategoryIndex = Math.floor(Math.random() * this.newsTypes.length);
      this.selectedType = this.newsTypes[this.currentCategoryIndex].type;
    }, this.rotationIntervalMs);
  }
}
