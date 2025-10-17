import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { ChartsModule } from 'ng2-charts';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MaterialModule } from './material/material.module';
import { FooterComponent } from './components/footer/footer.component';
import { HeaderComponent } from './components/header/header.component';
import { BannerComponent } from './components/dashboard/banner/banner.component';
import { CartContainerComponent } from './components/dashboard/cart-container/cart-container.component';
import { CartComponent } from './components/dashboard/cart-container/cart/cart.component';
import { DiscoverComponent } from './components/dashboard/discover/discover.component';
import { EducationalNavigatorComponent } from './components/dashboard/educational-navigator/educational-navigator.component';
import { GraphComponent } from './components/stock-details/graph/graph.component';
import { StockDetailsComponent } from './components/stock-details/stock-details.component';
import {
  BuyComponent,
  StockDetailsSidebarComponent,
} from './components/stock-details/stock-details-sidebar/stock-details-sidebar.component';
import { RiskAnalysisComponent } from './components/stock-details/risk-analysis/risk-analysis.component';
import { AboutCompanyComponent } from './components/stock-details/about-company/about-company.component';
import { PeerStocksComponent } from './components/stock-details/peer-stocks/peer-stocks.component';
import { NgwWowModule } from 'ngx-wow';
import { FormsModule } from '@angular/forms';
import { FlexLayoutModule } from '@angular/flex-layout';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { WatchlistComponent } from './components/watchlist/watchlist.component';
import { DialogOverviewExampleDialogComponent } from './components/stock-details/stock-details-sidebar/stock-details-sidebar.component';
import { LoginModalComponent } from './components/header/login-modal/login-modal.component';
import { ReactiveFormsModule } from '@angular/forms';
import { NewsComponent } from './components/dashboard/news-container/news/news.component';
import { NewsContainerComponent } from './components/dashboard/news-container/news-container.component';
import { PredictedPriceComponent } from './components/stock-details/predicted-price/predicted-price.component';
import { PaymentHistoryComponent } from './components/university/payment-history/payment-history.component';
import { UniversityComponent } from './components/university/university.component';
import { TopGainerComponent } from './components/watchlist/top-gainer/top-gainer.component';
import { TopLoserComponent } from './components/watchlist/top-loser/top-loser.component';
import { BestMonthComponent } from './components/watchlist/best-month/best-month.component';
import { RecommendationComponent } from './components/watchlist/recommendation/recommendation.component';
import { AllStockComponent } from './components/watchlist/all-stock/all-stock.component';
import { ToastrModule } from 'ngx-toastr';
import { TokenInterceptorService } from './services/token-interceptor.service';
import { MockStocksTableComponent } from './components/university/mock-stocks-table/mock-stocks-table.component';
import { BuySellModalComponent } from './components/university/buy-sell-modal/buy-sell-modal.component';
import { SignupModalComponent } from './components/header/signup-modal/signup-modal.component';
import { UserDashboardComponent } from './components/university/user-dashboard/user-dashboard.component';
import { StockOrderHistoryComponent } from './components/university/stock-order-history/stock-order-history.component';
import { SandboxComponent } from './components/university/sandbox/sandbox.component';
import { MatCardModule } from '@angular/material/card';
import { AddCreditsModalComponent } from './components/university/add-credits-modal/add-credits-modal.component';
import { StocksSellTableComponent } from './components/university/stocks-sell-table/stocks-sell-table.component';
import { MarketIntroComponent } from './components/university/market-intro/market-intro.component';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { LandingComponent } from './components/university/landing/landing.component';
import { ProfileComponent } from './components/profile/profile.component';
import { SubscriptionModalComponent } from './components/university/subscription-modal/subscription-modal.component';
import { ContactUsComponent } from './components/dashboard/contact-us/contact-us.component';
import { OurTeamComponent } from './components/dashboard/our-team/our-team.component';
import { environment } from '../environments/environment';
import { NGXLoggerModule } from 'ngx-logger';

@NgModule({
  declarations: [
    AppComponent,
    FooterComponent,
    HeaderComponent,
    BannerComponent,
    CartContainerComponent,
    CartComponent,
    DiscoverComponent,
    EducationalNavigatorComponent,
    GraphComponent,
    StockDetailsComponent,
    StockDetailsSidebarComponent,
    RiskAnalysisComponent,
    AboutCompanyComponent,
    PeerStocksComponent,
    DashboardComponent,
    WatchlistComponent,
    DialogOverviewExampleDialogComponent,
    BuyComponent,
    LoginModalComponent,
    NewsComponent,
    NewsContainerComponent,
    PredictedPriceComponent,
    SignupModalComponent,
    UserDashboardComponent,
    StockOrderHistoryComponent,
    PaymentHistoryComponent,
    UniversityComponent,
    SandboxComponent,
    TopGainerComponent,
    TopLoserComponent,
    BestMonthComponent,
    RecommendationComponent,
    AllStockComponent,
    MockStocksTableComponent,
    BuySellModalComponent,
    AddCreditsModalComponent,
    StocksSellTableComponent,
    MarketIntroComponent,
    LandingComponent,
    ProfileComponent,
    SubscriptionModalComponent,
    ContactUsComponent,
    OurTeamComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MaterialModule,
    FlexLayoutModule,
    ChartsModule,
    HttpClientModule,
    NgwWowModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    ToastrModule.forRoot(),
    NgbModule,
    NGXLoggerModule.forRoot({
      level: environment.logLevel,
      serverLoggingUrl: environment.serverLoggingUrl,
      serverLogLevel: environment.serverLogLevel,
    }),
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: TokenInterceptorService,
      multi: true,
    },
  ],
  bootstrap: [AppComponent],
  entryComponents: [LoginModalComponent],
})
export class AppModule {}
