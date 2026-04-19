import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-helps',
  templateUrl: './helps.component.html',
  styleUrls: ['./helps.component.css'],
  standalone: false
})
export class HelpsComponent implements OnInit {

  private http = inject(HttpClient);

  developerMarkdown = signal<string>('');
  marketingMarkdown = signal<string>('');

  activeTab = signal<'developer' | 'marketing'>('developer');

  loading = signal<boolean>(false);

  error = signal<string | null>(null);

  ngOnInit() {
    this.loadDeveloperDocs();
  }

  loadDeveloperDocs() {
    this.loading.set(true);
    this.error.set(null);

    this.http.get('/assets/docs/developer.md', { responseType: 'text' })
      .subscribe({
        next: (data) => {
          this.developerMarkdown.set(data);
          this.loading.set(false);
        },
        error: (err) => {
          console.error('Error loading developer docs:', err);
          this.error.set('Failed to load developer documentation');
          this.loading.set(false);
        }
      });
  }

  loadMarketingDocs() {
    this.loading.set(true);
    this.error.set(null);

    this.http.get('/assets/docs/marketing.md', { responseType: 'text' })
      .subscribe({
        next: (data) => {
          this.marketingMarkdown.set(data);
          this.loading.set(false);
        },
        error: (err) => {
          console.error('Error loading marketing docs:', err);
          this.error.set('Failed to load marketing documentation');
          this.loading.set(false);
        }
      });
  }

  switchTab(tab: 'developer' | 'marketing') {
    this.activeTab.set(tab);

    if (tab === 'developer' && !this.developerMarkdown()) {
      this.loadDeveloperDocs();
    }

    if (tab === 'marketing' && !this.marketingMarkdown()) {
      this.loadMarketingDocs();
    }
  }
}