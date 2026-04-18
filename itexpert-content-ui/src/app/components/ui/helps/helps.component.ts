// helps.component.ts
import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
    selector: 'app-helps',
    templateUrl: './helps.component.html',
    styleUrls: ['./helps.component.css'],
    standalone: false
})
export class HelpsComponent implements OnInit {
  developerMarkdown: string = '';
  marketingMarkdown: string = '';
  activeTab: 'developer' | 'marketing' = 'developer';
  loading: boolean = false;
  error: string | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadDeveloperDocs();
  }

  loadDeveloperDocs() {
    this.loading = true;
    this.error = null;

    this.http.get('/assets/docs/developer.md', { responseType: 'text' })
      .subscribe({
        next: (data) => {
          this.developerMarkdown = data;
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading developer docs:', err);
          this.error = 'Failed to load developer documentation';
          this.loading = false;
        }
      });
  }

  loadMarketingDocs() {
    this.loading = true;
    this.error = null;

    this.http.get('/assets/docs/marketing.md', { responseType: 'text' })
      .subscribe({
        next: (data) => {
          this.marketingMarkdown = data;
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading marketing docs:', err);
          this.error = 'Failed to load marketing documentation';
          this.loading = false;
        }
      });
  }

  switchTab(tab: 'developer' | 'marketing') {
    this.activeTab = tab;
    if (tab === 'developer' && !this.developerMarkdown) {
      this.loadDeveloperDocs();
    } else if (tab === 'marketing' && !this.marketingMarkdown) {
      this.loadMarketingDocs();
    }
  }
}
