import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { GainerLooserService } from 'src/app/services/gainer-looser.service';

interface FilterOption {
  label: string;
  value: string;
  selected: boolean;
}

interface StockDisplay {
  ticker: string;
  name: string;
  price: number;
  change: number;
  changePerc: number;
  sector: string;
  marketCapSize: string; // 'Large', 'Mid', 'Small'
}

@Component({
  selector: 'app-all-stock',
  templateUrl: './all-stock.component.html',
  styleUrls: ['./all-stock.component.css']
})
export class AllStockComponent implements OnInit {

  isLoading: boolean = true;
  searchQuery: string = '';
  
  // Master list of stocks
  private masterStocks: StockDisplay[] = [];
  filteredStocks: StockDisplay[] = [];

  // Filter state
  sectors: FilterOption[] = [
    { label: 'Technology', value: 'Technology', selected: false },
    { label: 'Finance', value: 'Finance', selected: false },
    { label: 'Healthcare', value: 'Healthcare', selected: false },
    { label: 'Consumer', value: 'Consumer', selected: false },
    { label: 'Automotive', value: 'Automotive', selected: false },
    { label: 'Energy', value: 'Energy', selected: false },
    { label: 'Other', value: 'Other', selected: false }
  ];

  marketCaps: FilterOption[] = [
    { label: 'Large Cap', value: 'Large', selected: false },
    { label: 'Mid Cap', value: 'Mid', selected: false },
    { label: 'Small Cap', value: 'Small', selected: false }
  ];

  constructor(
    private router: Router,
    private gainerLooserService: GainerLooserService
  ) { }

  ngOnInit(): void {
    window.scrollTo(0, 0);
    this.fetchRealTimeData();
  }

  fetchRealTimeData() {
    this.isLoading = true;
    
    // Fetching real-time market data from the backend to bypass Massive API's free tier limits
    this.gainerLooserService.getallStocks(0).subscribe({
      next: (result) => {
        if (result && result.data) {
          this.masterStocks = result.data.map((element: any) => {
            // Assign a mock sector/market cap for the UI filters since backend doesn't provide them directly
            let sector = 'Other';
            if (['AAPL', 'MSFT', 'GOOGL', 'NVDA', 'META', 'CRM', 'ADBE'].includes(element.symbol)) sector = 'Technology';
            else if (['JPM', 'BAC', 'V', 'MA', 'GS'].includes(element.symbol)) sector = 'Finance';
            else if (['JNJ', 'UNH', 'PFE', 'ABBV'].includes(element.symbol)) sector = 'Healthcare';
            else if (['AMZN', 'WMT', 'PG', 'KO', 'PEP'].includes(element.symbol)) sector = 'Consumer';
            else if (['TSLA', 'F', 'GM'].includes(element.symbol)) sector = 'Automotive';
            else if (['XOM', 'CVX'].includes(element.symbol)) sector = 'Energy';

            return {
              ticker: element.symbol,
              name: element.longName || element.symbol,
              price: parseFloat(element.currentPrice?.raw || element.currentPrice?.fmt?.replace(/,/g, '') || '0'),
              change: parseFloat(element.change?.raw || element.change?.fmt?.replace(/,/g, '') || '0'),
              changePerc: parseFloat(element.changePercentage?.raw || element.changePercentage?.fmt?.replace(/[%+,]/g, '') || '0'),
              sector: sector,
              marketCapSize: 'Large'
            };
          });
        }
        
        // Also fetch page 1 to get more stocks for the screener
        this.gainerLooserService.getallStocks(1).subscribe({
          next: (res2) => {
            if (res2 && res2.data) {
              const page1 = res2.data.map((element: any) => {
                return {
                  ticker: element.symbol,
                  name: element.longName || element.symbol,
                  price: parseFloat(element.currentPrice?.raw || element.currentPrice?.fmt?.replace(/,/g, '') || '0'),
                  change: parseFloat(element.change?.raw || element.change?.fmt?.replace(/,/g, '') || '0'),
                  changePerc: parseFloat(element.changePercentage?.raw || element.changePercentage?.fmt?.replace(/[%+,]/g, '') || '0'),
                  sector: 'Other',
                  marketCapSize: 'Mid'
                };
              });
              this.masterStocks = [...this.masterStocks, ...page1];
            }
            this.applyFilters();
            this.isLoading = false;
          },
          error: () => {
            this.applyFilters();
            this.isLoading = false;
          }
        });
      },
      error: (err) => {
        console.error('Failed to fetch real time stocks', err);
        this.isLoading = false;
      }
    });
  }

  applyFilters() {
    const selectedSectors = this.sectors.filter(s => s.selected).map(s => s.value);
    const selectedCaps = this.marketCaps.filter(c => c.selected).map(c => c.value);
    const searchLower = this.searchQuery.toLowerCase();

    this.filteredStocks = this.masterStocks.filter(stock => {
      // 1. Search filter
      const matchesSearch = stock.ticker.toLowerCase().includes(searchLower) || stock.name.toLowerCase().includes(searchLower);
      
      // 2. Sector filter
      const matchesSector = selectedSectors.length === 0 || selectedSectors.includes(stock.sector);

      // 3. Market Cap filter
      const matchesCap = selectedCaps.length === 0 || selectedCaps.includes(stock.marketCapSize);

      return matchesSearch && matchesSector && matchesCap;
    });
  }

  onSearchChange() {
    this.applyFilters();
  }

  toggleFilter(filterList: FilterOption[], filter: FilterOption) {
    filter.selected = !filter.selected;
    this.applyFilters();
  }

  clearAllFilters() {
    this.sectors.forEach(s => s.selected = false);
    this.marketCaps.forEach(c => c.selected = false);
    this.searchQuery = '';
    this.applyFilters();
  }

  navigateToStock(ticker: string) {
    this.router.navigate(['/stockDetails/' + ticker]);
  }

  formatChange(val: number): string {
    return val > 0 ? `+${val.toFixed(2)}` : val.toFixed(2);
  }

  getSelectedCount(): number {
    return this.sectors.filter(s => s.selected).length + this.marketCaps.filter(c => c.selected).length;
  }
}
