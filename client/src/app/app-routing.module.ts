import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { GraphComponent } from './components/stock-details/graph/graph.component';
import { UniversityComponent } from './components/university/university.component';
import { StockDetailsComponent } from './components/stock-details/stock-details.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { WatchlistComponent } from './components/watchlist/watchlist.component';
import { UserDashboardComponent } from './components/university/user-dashboard/user-dashboard.component';
import { SandboxComponent } from './components/university/sandbox/sandbox.component';
import { TopGainerComponent } from './components/watchlist/top-gainer/top-gainer.component';
import { TopLoserComponent } from './components/watchlist/top-loser/top-loser.component';
import { RecommendationComponent } from './components/watchlist/recommendation/recommendation.component';
import { BestMonthComponent } from './components/watchlist/best-month/best-month.component';
import { AllStockComponent } from './components/watchlist/all-stock/all-stock.component';
import { MockStocksTableComponent } from './components/university/mock-stocks-table/mock-stocks-table.component';
import { StockOrderHistoryComponent } from './components/university/stock-order-history/stock-order-history.component';
import { PaymentHistoryComponent } from './components/university/payment-history/payment-history.component';
import { SignupModalComponent } from './components/header/signup-modal/signup-modal.component';
import { AuthGuard } from './services/auth-guard.service';
import { MarketIntroComponent } from './components/university/market-intro/market-intro.component';
import { LandingComponent } from './components/university/landing/landing.component';
import { ProfileComponent } from './components/profile/profile.component';

const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'home', component: DashboardComponent },
  { path: 'stockDetails/:symbol', component: StockDetailsComponent },
  { path: 'graph/:symbol', component: GraphComponent },
  { path: 'signup', component: SignupModalComponent },
  { path: 'userDashboard', component: UserDashboardComponent },
  { path: 'discover', component: WatchlistComponent },
  { path: 'user/profile', canActivate: [AuthGuard], component: ProfileComponent },
  {
    path: 'user/sandbox',
    canActivate: [AuthGuard],
    component: SandboxComponent,
    children: [
      { path: 'dashboard', component: UserDashboardComponent },
      { path: 'orders', component: StockOrderHistoryComponent },
      { path: 'credits', component: PaymentHistoryComponent },
      { path: 'stocks', component: MockStocksTableComponent },
    ],
  },
  { path: 'mockStocks', component: MockStocksTableComponent },
  {
    path: 'university',
    component: UniversityComponent,
    children: [
      { path: '', component: LandingComponent },
      { path: 'market-intro', component: MarketIntroComponent },
    ],
  },
  {
    path: 'discover',
    component: WatchlistComponent,
    children: [
      { path: 'top-gainer', component: TopGainerComponent },
      { path: 'top-loser', component: TopLoserComponent },
      { path: 'recommendation', component: RecommendationComponent },
      { path: 'best-month', component: BestMonthComponent },
      { path: 'all-stocks', component: AllStockComponent },
    ],
  },
  { path: '**', redirectTo: '/home' },
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, {
      anchorScrolling: 'enabled',
    }),
  ],
  exports: [RouterModule],
})
export class AppRoutingModule {}
