import { Injectable } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { filter } from 'rxjs/operators';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class EventTrackingService {

  private apiUrl = environment.serverUrl + '/events';

  constructor(
    private router: Router,
    private http: HttpClient
  ) {}

  public init(): void {
    // Listen to all router navigation events to track page views
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      this.trackPageView(event.urlAfterRedirects);
    });
  }

  public trackEvent(eventType: string, eventInfo: any = {}): void {
    const userId = this.getUserId();
    
    const payload = {
      event_type: eventType,
      user_id: userId,
      event_info: eventInfo
    };

    // Fire and forget
    this.http.post(this.apiUrl, payload).subscribe({
      next: () => {},
      error: (err) => console.debug('Event tracking failed', err)
    });
  }

  private trackPageView(url: string): void {
    const userId = this.getUserId();
    
    const payload = {
      event_type: 'PAGE_VIEW',
      user_id: userId,
      event_info: {
        pagePath: url,
        referrer: document.referrer || '',
        title: document.title
      }
    };

    // Fire and forget
    this.http.post(this.apiUrl, payload).subscribe({
      next: () => {},
      error: (err) => console.debug('Event tracking failed', err)
    });
  }

  private getUserId(): number | null {
    try {
      const userStr = localStorage.getItem('user');
      if (userStr) {
        const user = JSON.parse(userStr);
        return user.id || null;
      }
    } catch (e) {
      // Ignore parse errors
    }
    return null;
  }
}
